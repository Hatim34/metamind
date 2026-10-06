package be.icc.metamind.credit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.api.ClientIpResolver;
import be.icc.metamind.document.AuditLogEntity;
import be.icc.metamind.document.AuditLogRepository;
import be.icc.metamind.document.CreditPackEntity;
import be.icc.metamind.document.CreditPackRepository;
import be.icc.metamind.document.CreditPackStatus;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {
	private static final Logger log = LoggerFactory.getLogger(CreditService.class);
	private static final List<CreditPackOptionResponse> PACK_OPTIONS = List.of(
			new CreditPackOptionResponse(2, 100, new BigDecimal("50.00"), "EUR", "Pack standard"),
			new CreditPackOptionResponse(3, 500, new BigDecimal("200.00"), "EUR", "Pack volume")
	);

	private final UserRepository userRepository;
	private final InstitutionRepository institutionRepository;
	private final CreditMovementRepository movementRepository;
	private final CreditPackRepository packRepository;
	private final AuditLogRepository auditLogRepository;
	private final PaymentConsentRepository paymentConsentRepository;
	private final StripeProcessedEventRepository stripeProcessedEventRepository;
	private final String publicUrl;
	private final String stripeSecretKey;
	private final String stripeWebhookSecret;

	public CreditService(
			UserRepository userRepository,
			InstitutionRepository institutionRepository,
			CreditMovementRepository movementRepository,
			CreditPackRepository packRepository,
			AuditLogRepository auditLogRepository,
			PaymentConsentRepository paymentConsentRepository,
			StripeProcessedEventRepository stripeProcessedEventRepository,
		@Value("${metamind.public-url:https://metamind-app.duckdns.org}") String publicUrl,
		@Value("${metamind.stripe.secret-key:}") String stripeSecretKey,
		@Value("${metamind.stripe.webhook-secret:}") String stripeWebhookSecret,
		@Value("${spring.profiles.active:}") String activeProfile
	) {
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.movementRepository = movementRepository;
		this.packRepository = packRepository;
		this.auditLogRepository = auditLogRepository;
		this.paymentConsentRepository = paymentConsentRepository;
		this.stripeProcessedEventRepository = stripeProcessedEventRepository;
		this.publicUrl = publicUrl;
		this.stripeSecretKey = stripeSecretKey == null ? "" : stripeSecretKey.trim();
		this.stripeWebhookSecret = stripeWebhookSecret == null ? "" : stripeWebhookSecret.trim();
		if ("prod".equalsIgnoreCase(activeProfile) && !this.stripeSecretKey.isBlank()
				&& this.stripeWebhookSecret.isBlank()) {
			throw new IllegalStateException("STRIPE_WEBHOOK_SECRET est obligatoire en production.");
		}
	}

	@Transactional(readOnly = true)
	public CreditBalanceResponse getBalance(long userId) {
		return toResponse(findUser(userId).getInstitution());
	}

	@Transactional(readOnly = true)
	public List<CreditMovementResponse> listMovements(long userId) {
		InstitutionEntity institution = findUser(userId).getInstitution();
		return movementRepository.findByInstitutionIdOrderByCreatedAtDesc(institution.getId()).stream()
				.map(CreditMovementResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public CreditAccountResponse getCurrentAccount(UserEntity user) {
		return new CreditAccountResponse(toResponse(user.getInstitution()), listMovements(user.getId()));
	}

	public List<CreditPackOptionResponse> listPacks() {
		return PACK_OPTIONS;
	}

	@Transactional
	public CreditCheckoutResponse startCheckout(UserEntity user, CreditCheckoutRequest request) {
		CreditPackOptionResponse option = findPackOption(request.packId());
		if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.GESTIONNAIRE_FINANCIER) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Le role gestionnaire financier est requis pour acheter des credits.");
		}
		if (user.getInstitution().isPurchasesSuspended()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Les achats de credits sont suspendus pour cette institution.");
		}
		savePaymentConsents(user);
		String reference = "pay_" + UUID.randomUUID().toString().replace("-", "");
		CreditPackEntity pack = packRepository.save(new CreditPackEntity(
				user.getInstitution(),
				option.credits(),
				option.amount(),
				reference,
				CreditPackStatus.EN_ATTENTE
		));
		String checkoutUrl = createCheckoutUrl(user, option, reference, pack.getId());
		return new CreditCheckoutResponse(checkoutUrl, reference);
	}

	@Transactional(readOnly = true)
	public CreditCheckoutStatusResponse getCheckoutStatus(UserEntity user, String reference) {
		CreditPackEntity pack = packRepository.findByPaymentReference(reference)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La reference de paiement est introuvable."));
		if (pack.getInstitution().getId() == null || !pack.getInstitution().getId().equals(user.getInstitution().getId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, "La reference de paiement est introuvable.");
		}
		return new CreditCheckoutStatusResponse(reference, pack.getStatus().name(), pack.getInstitution().getCreditBalance());
	}

	private void savePaymentConsents(UserEntity user) {
		Instant acceptedAt = Instant.now();
		String ip = ClientIpResolver.current();
		paymentConsentRepository.save(new PaymentConsentEntity(user, ConsentType.CGV, "2026-01", acceptedAt, ip));
		paymentConsentRepository.save(new PaymentConsentEntity(user, ConsentType.RENONCIATION_RETRACTATION, "2026-01", acceptedAt, ip));
	}

	@Transactional
	public CreditBalanceResponse confirmStripePayment(StripeWebhookRequest request) {
		return confirmPaymentReference(request.reference(), request.type(), request.eventId(), request.amountTotal(), request.currency());
	}

	@Transactional
	public CreditBalanceResponse confirmStripePayment(String payload, String signature) {
		StripeWebhookRequest request = parseWebhookPayload(payload, signature);
		return confirmPaymentReference(request.reference(), request.type(), request.eventId(), request.amountTotal(), request.currency());
	}

	private CreditBalanceResponse confirmPaymentReference(String reference, String type, String eventId, Long amountTotal, String currency) {
		CreditPackEntity pack = packRepository.findByPaymentReference(reference)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La reference de paiement est introuvable."));
		if (eventId != null && stripeProcessedEventRepository.existsByEventId(eventId)) {
			return toResponse(pack.getInstitution());
		}
		if (eventId != null && !matchesPackAmount(pack, amountTotal, currency)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le montant du paiement ne correspond pas au pack.");
		}
		if (!isCompletedPayment(type)) {
			pack.markFailed();
			markStripeEvent(eventId, type);
			return toResponse(pack.getInstitution());
		}
		if (!pack.isPaid()) {
			pack.markPaid();
			addPurchasedCredits(pack.getInstitution(), pack.getQuantite(), "Paiement confirme " + pack.getPaymentReference());
		}
		markStripeEvent(eventId, type);
		return toResponse(pack.getInstitution());
	}

	private boolean matchesPackAmount(CreditPackEntity pack, Long amountTotal, String currency) {
		if (amountTotal == null || currency == null) {
			return false;
		}
		long expectedCents = pack.getPaidAmount().movePointRight(2).longValueExact();
		return expectedCents == amountTotal && "eur".equalsIgnoreCase(currency);
	}

	private void markStripeEvent(String eventId, String eventType) {
		if (eventId != null && !eventId.isBlank()) {
			stripeProcessedEventRepository.save(new StripeProcessedEventEntity(eventId, eventType, Instant.now()));
		}
	}

	private String createCheckoutUrl(UserEntity user, CreditPackOptionResponse option, String reference, long packId) {
		if (!isStripeEnabled()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Le paiement Stripe n'est pas configure.");
		}
		Stripe.apiKey = stripeSecretKey;
		try {
			SessionCreateParams.ConsentCollection consentCollection = SessionCreateParams.ConsentCollection.builder()
					.setTermsOfService(SessionCreateParams.ConsentCollection.TermsOfService.REQUIRED)
					.build();
			SessionCreateParams params = SessionCreateParams.builder()
					.setMode(SessionCreateParams.Mode.PAYMENT)
					.setClientReferenceId(reference)
					.setCustomerEmail(user.getEmail())
					.setConsentCollection(consentCollection)
					.putMetadata("institutionId", user.getInstitution().getId().toString())
					.putMetadata("packId", Long.toString(packId))
					.putMetadata("paymentReference", reference)
					.setSuccessUrl(publicUrl + "/paiement/succes?session_id={CHECKOUT_SESSION_ID}")
					.setCancelUrl(publicUrl + "/credits")
					.addLineItem(SessionCreateParams.LineItem.builder()
							.setQuantity(1L)
							.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
									.setCurrency(option.currency().toLowerCase())
									.setUnitAmount(option.amount().multiply(new BigDecimal("100")).longValueExact())
									.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
											.setName(option.label())
											.build())
									.build())
							.build())
					.build();
			Session session = Session.create(params);
			return session.getUrl();
		} catch (StripeException exception) {
			// Le detail reste dans les journaux Render, jamais dans la reponse HTTP.
			log.warn("Creation Checkout Stripe refusee (statut {}, type {}) : {}",
					exception.getStatusCode(), exception.getClass().getSimpleName(), exception.getMessage());
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La session de paiement n'a pas pu etre creee.");
		} catch (ArithmeticException exception) {
			log.warn("Montant Stripe invalide pour le pack : {}", exception.getMessage());
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La session de paiement n'a pas pu etre creee.");
		}
	}

	private StripeWebhookRequest parseWebhookPayload(String payload, String signature) {
		if (payload == null || payload.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le contenu du webhook est vide.");
		}
		if (stripeWebhookSecret.isBlank()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Le webhook Stripe n'est pas configure.");
		}
		if (signature == null || signature.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La signature Stripe est manquante.");
		}
		try {
			Webhook.constructEvent(payload, signature, stripeWebhookSecret);
		} catch (SignatureVerificationException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La signature Stripe est invalide.");
		}
		try {
			JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
			String eventId = text(root, "id");
			String type = text(root, "type");
			Long amountTotal = number(object(root, "data", "object"), "amount_total");
			String currency = text(object(root, "data", "object"), "currency");
			String reference = text(root, "reference");
			JsonObject object = root.has("data") && root.get("data").isJsonObject()
					? root.getAsJsonObject("data").getAsJsonObject("object")
					: null;
			if (reference == null) {
				reference = text(object, "client_reference_id");
			}
			if (reference == null) {
				reference = text(object, "reference");
			}
			if (reference == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "La reference de paiement est manquante.");
			}
			if (eventId == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "L'identifiant de l'evenement Stripe est manquant.");
			}
			return new StripeWebhookRequest(reference, type, eventId, amountTotal, currency);
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le contenu du webhook est invalide.");
		}
	}

	private JsonObject object(JsonObject root, String parent, String child) {
		if (root == null || !root.has(parent) || !root.get(parent).isJsonObject()) {
			return null;
		}
		JsonObject parentObject = root.getAsJsonObject(parent);
		return parentObject.has(child) && parentObject.get(child).isJsonObject()
				? parentObject.getAsJsonObject(child) : null;
	}

	private Long number(JsonObject node, String field) {
		if (node == null || !node.has(field) || node.get(field).isJsonNull()) {
			return null;
		}
		return node.get(field).getAsLong();
	}

	private String text(JsonObject node, String field) {
		if (node == null || !node.has(field)) {
			return null;
		}
		JsonElement element = node.get(field);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		String value = element.getAsString();
		return value.isBlank() ? null : value;
	}

	private boolean isStripeEnabled() {
		return !stripeSecretKey.isBlank();
	}

	@Transactional
	public CreditBalanceResponse adjustInstitutionCredits(UserEntity admin, long institutionId, AdminCreditAdjustmentRequest request) {
		if (request.amount() == 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "L'ajustement de credits ne peut pas etre nul.");
		}
		InstitutionEntity institution = institutionRepository.findById(institutionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution est introuvable."));
		if (request.amount() > 0) {
			institution.addCredits(request.amount());
		} else {
			for (int credit = 0; credit < Math.abs(request.amount()); credit++) {
				if (!institution.hasCredits()) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Le solde de credits ne peut pas devenir negatif.");
				}
				institution.consumeCredit();
			}
		}
		CreditMovementType type = CreditMovementType.AJUSTEMENT_ADMIN;
		movementRepository.save(new CreditMovementEntity(
				institution,
				type,
				request.amount(),
				institution.getCreditBalance(),
				normalReason(request.reason())
		));
		auditLogRepository.save(new AuditLogEntity(
				admin,
				"AJUSTEMENT_CREDITS",
				"institutions",
				institutionId,
				normalReason(request.reason()),
				ClientIpResolver.current()
		));
		return toResponse(institution);
	}

	private UserEntity findUser(long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
	}

	private CreditPackOptionResponse findPackOption(int id) {
		return PACK_OPTIONS.stream()
				.filter(option -> option.id() == id)
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le pack de credits est introuvable."));
	}

	private void addPurchasedCredits(InstitutionEntity institution, int amount, String description) {
		institution.addCredits(amount);
		movementRepository.save(new CreditMovementEntity(
				institution,
				CreditMovementType.ACHAT,
				amount,
				institution.getCreditBalance(),
				description
		));
	}

	private boolean isCompletedPayment(String type) {
		return "checkout.session.completed".equals(type)
				|| "checkout.session.async_payment_succeeded".equals(type);
	}

	private String normalReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return "Ajustement manuel des credits";
		}
		return reason.trim();
	}

	private CreditBalanceResponse toResponse(InstitutionEntity institution) {
		return new CreditBalanceResponse(institution.getId(), institution.getName(), institution.getCreditBalance());
	}
}

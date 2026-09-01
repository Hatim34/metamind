package be.icc.metamind.credit;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.AuditLogEntity;
import be.icc.metamind.document.AuditLogRepository;
import be.icc.metamind.document.CreditPackEntity;
import be.icc.metamind.document.CreditPackRepository;
import be.icc.metamind.document.CreditPackStatus;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {
	private static final List<CreditPackOptionResponse> PACK_OPTIONS = List.of(
			new CreditPackOptionResponse(1, 20, new BigDecimal("0.00"), "EUR", "Pack decouverte"),
			new CreditPackOptionResponse(2, 100, new BigDecimal("50.00"), "EUR", "Pack standard"),
			new CreditPackOptionResponse(3, 500, new BigDecimal("200.00"), "EUR", "Pack volume")
	);

	private final UserRepository userRepository;
	private final InstitutionRepository institutionRepository;
	private final CreditMovementRepository movementRepository;
	private final CreditPackRepository packRepository;
	private final AuditLogRepository auditLogRepository;
	private final String publicUrl;

	public CreditService(
			UserRepository userRepository,
			InstitutionRepository institutionRepository,
			CreditMovementRepository movementRepository,
			CreditPackRepository packRepository,
			AuditLogRepository auditLogRepository,
			@Value("${metamind.public-url:https://metamind-app.duckdns.org}") String publicUrl
	) {
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.movementRepository = movementRepository;
		this.packRepository = packRepository;
		this.auditLogRepository = auditLogRepository;
		this.publicUrl = publicUrl;
	}

	@Transactional(readOnly = true)
	public CreditBalanceResponse getBalance(long userId) {
		return toResponse(findUser(userId).getInstitution());
	}

	@Transactional
	public CreditBalanceResponse purchase(long userId, CreditPurchaseRequest request) {
		InstitutionEntity institution = findUser(userId).getInstitution();
		addPurchasedCredits(institution, request.amount(), "Achat de credits");
		return toResponse(institution);
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
		String reference = "pay_" + UUID.randomUUID().toString().replace("-", "");
		CreditPackEntity pack = packRepository.save(new CreditPackEntity(
				user.getInstitution(),
				option.credits(),
				option.amount(),
				reference,
				CreditPackStatus.EN_ATTENTE
		));
		String checkoutUrl = publicUrl + "/paiement/confirmation?reference=" + reference + "&pack=" + pack.getId();
		return new CreditCheckoutResponse(checkoutUrl, reference);
	}

	@Transactional
	public CreditBalanceResponse confirmStripePayment(StripeWebhookRequest request) {
		CreditPackEntity pack = packRepository.findByPaymentReference(request.reference())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La reference de paiement est introuvable."));
		if (!isCompletedPayment(request.type())) {
			pack.markFailed();
			return toResponse(pack.getInstitution());
		}
		if (!pack.isPaid()) {
			pack.markPaid();
			addPurchasedCredits(pack.getInstitution(), pack.getQuantite(), "Paiement confirme " + pack.getPaymentReference());
		}
		return toResponse(pack.getInstitution());
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
		CreditMovementType type = request.amount() > 0 ? CreditMovementType.ACHAT : CreditMovementType.CONSOMMATION;
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
				"system"
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
		return type == null || type.isBlank() || type.equals("checkout.session.completed");
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

package be.icc.metamind.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.api.ClientIpResolver;
import be.icc.metamind.document.AuditLogEntity;
import be.icc.metamind.document.AuditLogRepository;
import be.icc.metamind.document.CreditPackEntity;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Factures des achats de credits. Une facture est creee a la confirmation du paiement ; elle recoit son
 * numero et son XML Peppol des que les donnees de facturation de l'institution sont completes.
 */
@Service
public class InvoiceService {
	static final BigDecimal VAT_RATE = new BigDecimal("21.00");

	private final InvoiceRepository invoiceRepository;
	private final InvoiceCounterRepository counterRepository;
	private final InstitutionRepository institutionRepository;
	private final AuditLogRepository auditLogRepository;
	private final UblInvoiceWriter ublWriter;
	private final InvoicePdfWriter pdfWriter;

	public InvoiceService(InvoiceRepository invoiceRepository, InvoiceCounterRepository counterRepository,
			InstitutionRepository institutionRepository, AuditLogRepository auditLogRepository,
			UblInvoiceWriter ublWriter, InvoicePdfWriter pdfWriter) {
		this.invoiceRepository = invoiceRepository;
		this.counterRepository = counterRepository;
		this.institutionRepository = institutionRepository;
		this.auditLogRepository = auditLogRepository;
		this.ublWriter = ublWriter;
		this.pdfWriter = pdfWriter;
	}

	/** Appelee quand Stripe confirme un paiement. Un achat n'a jamais deux factures. */
	@Transactional
	public InvoiceEntity createFor(CreditPackEntity pack) {
		return invoiceRepository.findByPack_Id(pack.getId()).orElseGet(() -> {
			BigDecimal gross = pack.getPaidAmount().setScale(2, RoundingMode.HALF_UP);
			BigDecimal net = netAmountFor(gross);
			InvoiceEntity invoice = invoiceRepository.save(new InvoiceEntity(pack, LocalDate.now(),
					"Pack de " + pack.getQuantite() + " credits d'analyse Metamind", net, VAT_RATE, gross.subtract(net), gross, "EUR"));
			if (pack.getInstitution().hasCompleteBilling()) {
				issue(invoice);
			}
			return invoice;
		});
	}

	/**
	 * Le prix paye est TTC. Le montant HT retenu est celui dont la TVA arrondie redonne exactement
	 * le prix paye : la facture respecte ainsi la regle europeenne HT + TVA = TTC, au centime pres.
	 */
	static BigDecimal netAmountFor(BigDecimal gross) {
		BigDecimal estimate = gross.multiply(new BigDecimal("100")).divide(new BigDecimal("121"), 2, RoundingMode.HALF_UP);
		for (int offset = -2; offset <= 2; offset++) {
			BigDecimal net = estimate.add(new BigDecimal(offset).movePointLeft(2));
			BigDecimal vat = net.multiply(VAT_RATE).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
			if (net.add(vat).compareTo(gross) == 0) {
				return net;
			}
		}
		return estimate;
	}

	private void issue(InvoiceEntity invoice) {
		int year = LocalDate.now().getYear();
		InvoiceCounterEntity counter = counterRepository.lockYear(year)
				.orElseGet(() -> counterRepository.saveAndFlush(new InvoiceCounterEntity(year)));
		invoice.markIssued(year, counter.next(), LocalDate.now(), invoice.getInstitution());
		invoice.attachXml(ublWriter.write(invoice));
		auditLogRepository.save(new AuditLogEntity(null, "EMISSION_FACTURE", "institutions", invoice.getInstitution().getId(),
				"Facture " + invoice.getNumber() + " : " + invoice.getGrossAmount().toPlainString() + " EUR TTC", ClientIpResolver.current()));
	}

	/** L'administrateur renseigne les donnees de facturation ; les factures en attente sont alors emises. */
	@Transactional
	public InstitutionResponse updateBilling(long institutionId, InstitutionBillingRequest request, UserEntity admin) {
		InstitutionEntity institution = institutionRepository.findById(institutionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution est introuvable."));
		String enterpriseNumber = EnterpriseNumber.normalize(request.enterpriseNumber());
		if (!EnterpriseNumber.isValid(enterpriseNumber)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le numero d'entreprise n'est pas valide (dix chiffres, par exemple 0123.456.749).");
		}
		String country = request.pays() == null || request.pays().isBlank() ? "BE" : request.pays().trim().toUpperCase();
		if (!"BE".equals(country)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Seules les institutions belges peuvent etre facturees pour le moment.");
		}
		institution.updateBilling(request.legalName().trim(), enterpriseNumber, request.street().trim(), request.postalCode().trim(),
				request.ville().trim(), country, request.billingEmail() == null ? null : request.billingEmail().trim());
		auditLogRepository.save(new AuditLogEntity(admin, "MODIFICATION_INSTITUTION", "institutions", institution.getId(),
				"Donnees de facturation renseignees (BCE " + EnterpriseNumber.format(enterpriseNumber) + ")", ClientIpResolver.current()));
		invoiceRepository.findByInstitution_IdOrderByIdDesc(institution.getId()).stream()
				.filter(invoice -> invoice.getStatus() == InvoiceStatus.A_COMPLETER)
				.sorted((first, second) -> first.getId().compareTo(second.getId()))
				.forEach(this::issue);
		return InstitutionResponse.from(institution);
	}

	/** Le bibliothecaire voit les factures de son institution, l'administrateur toutes. */
	@Transactional(readOnly = true)
	public List<InvoiceResponse> list(UserEntity user) {
		List<InvoiceEntity> invoices = user.getRole() == UserRole.ADMIN
				? invoiceRepository.findAllByOrderByIdDesc()
				: invoiceRepository.findByInstitution_IdOrderByIdDesc(user.getInstitution().getId());
		return invoices.stream().map(InvoiceResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public byte[] pdf(long id, UserEntity user) {
		return pdfWriter.write(issued(id, user));
	}

	@Transactional(readOnly = true)
	public String xml(long id, UserEntity user) {
		return issued(id, user).getUblXml();
	}

	@Transactional(readOnly = true)
	public String number(long id, UserEntity user) {
		return issued(id, user).getNumber();
	}

	private InvoiceEntity issued(long id, UserEntity user) {
		InvoiceEntity invoice = invoiceRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La facture est introuvable."));
		if (user.getRole() != UserRole.ADMIN && !invoice.getInstitution().getId().equals(user.getInstitution().getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette facture appartient a une autre institution.");
		}
		if (invoice.getStatus() != InvoiceStatus.EMISE) {
			throw new ApiException(HttpStatus.CONFLICT, "Facture a completer : les donnees de facturation de l'institution manquent.");
		}
		return invoice;
	}
}

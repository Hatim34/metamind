package be.icc.metamind.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.credit.CreditService;
import be.icc.metamind.credit.StripeWebhookRequest;
import be.icc.metamind.document.CreditPackEntity;
import be.icc.metamind.document.CreditPackRepository;
import be.icc.metamind.document.CreditPackStatus;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.PasswordService;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.peppol.PeppolValidation;
import com.helger.phive.peppol.PeppolValidation2026_11;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class InvoiceServiceTests {
	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private CreditPackRepository packRepository;

	@Autowired
	private CreditService creditService;

	@Autowired
	private InvoiceService invoiceService;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Test
	void paymentWithoutBillingDataWaitsThenIsIssuedOnceTheAdministratorCompletesIt() {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		UserEntity admin = saveUser("INST-ADM", "plateforme.example", "admin@plateforme.example", UserRole.ADMIN);
		pay(librarian, "pay_sans_facturation", "50.00");

		InvoiceResponse waiting = invoiceService.list(librarian).getFirst();
		assertThat(waiting.statut()).isEqualTo(InvoiceStatus.A_COMPLETER);
		assertThat(waiting.numero()).isNull();
		assertThatThrownBy(() -> invoiceService.pdf(waiting.id(), librarian))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("Facture a completer");

		invoiceService.updateBilling(librarian.getInstitution().getId(), billing("0987654394"), admin);

		InvoiceResponse issued = invoiceService.list(librarian).getFirst();
		assertThat(issued.statut()).isEqualTo(InvoiceStatus.EMISE);
		assertThat(issued.numero()).matches("\\d{4}-0001");
		assertThat(issued.netAmount()).isEqualByComparingTo("41.32");
		assertThat(issued.vatAmount()).isEqualByComparingTo("8.68");
		assertThat(issued.grossAmount()).isEqualByComparingTo("50.00");
	}

	@Test
	void invoicesAreNumberedWithoutGapAndEachPaymentHasOnlyOne() {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		completeBilling(librarian.getInstitution());
		pay(librarian, "pay_un", "50.00");
		creditService.confirmStripePayment(new StripeWebhookRequest("pay_un", "checkout.session.completed"));
		pay(librarian, "pay_deux", "200.00");

		assertThat(invoiceRepository.count()).isEqualTo(2);
		assertThat(invoiceService.list(librarian)).extracting(InvoiceResponse::numero)
				.allMatch(number -> number.matches("\\d{4}-000[12]"))
				.doesNotHaveDuplicates();
	}

	@Test
	void anotherInstitutionCannotDownloadTheInvoice() {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		UserEntity stranger = saveUser("INST-B", "institution-b.example", "paul@institution-b.example", UserRole.LIBRARIAN);
		completeBilling(librarian.getInstitution());
		pay(librarian, "pay_prive", "50.00");
		long id = invoiceService.list(librarian).getFirst().id();

		assertThat(invoiceService.list(stranger)).isEmpty();
		assertThatThrownBy(() -> invoiceService.xml(id, stranger))
				.isInstanceOf(ApiException.class)
				.hasMessage("Cette facture appartient a une autre institution.");
	}

	@Test
	void invalidEnterpriseNumberIsRefused() {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		UserEntity admin = saveUser("INST-ADM", "plateforme.example", "admin@plateforme.example", UserRole.ADMIN);

		assertThatThrownBy(() -> invoiceService.updateBilling(librarian.getInstitution().getId(), billing("0987654321"), admin))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("numero d'entreprise n'est pas valide");
	}

	@Test
	void issuedInvoicePassesTheOfficialPeppolValidation() throws Exception {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		completeBilling(librarian.getInstitution());
		pay(librarian, "pay_peppol", "200.00");
		InvoiceResponse invoice = invoiceService.list(librarian).getFirst();

		ValidationResultList result = validatePeppol(invoiceService.xml(invoice.id(), librarian));

		assertThat(result.getAllErrors().getAllTexts(java.util.Locale.FRENCH)).isEmpty();
		assertThat(result.containsAtLeastOneError()).isFalse();
	}

	@Test
	void validatorRejectsAnInvoiceWhoseTotalsDoNotAddUp() throws Exception {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		completeBilling(librarian.getInstitution());
		pay(librarian, "pay_fausse", "50.00");
		String xml = invoiceService.xml(invoiceService.list(librarian).getFirst().id(), librarian);

		String wrongTotal = xml.replace("<cbc:TaxInclusiveAmount currencyID=\"EUR\">50.00", "<cbc:TaxInclusiveAmount currencyID=\"EUR\">60.00");

		assertThat(wrongTotal).isNotEqualTo(xml);
		assertThat(validatePeppol(wrongTotal).containsAtLeastOneError()).isTrue();
	}

	@Test
	void pdfShowsTheSameAmountsAsTheXml() throws Exception {
		UserEntity librarian = saveUser("INST-A", "institution-a.example", "sarah@institution-a.example", UserRole.LIBRARIAN);
		completeBilling(librarian.getInstitution());
		pay(librarian, "pay_pdf", "50.00");
		InvoiceResponse invoice = invoiceService.list(librarian).getFirst();

		String text;
		try (var document = Loader.loadPDF(invoiceService.pdf(invoice.id(), librarian))) {
			text = new PDFTextStripper().getText(document);
		}

		java.nio.file.Files.write(java.nio.file.Path.of("/private/tmp/claude-502/-Users-assalhatim/10df42c0-a10a-458f-a53f-73bdce906408/scratchpad/facture.pdf"), invoiceService.pdf(invoice.id(), librarian));
		java.nio.file.Files.writeString(java.nio.file.Path.of("/private/tmp/claude-502/-Users-assalhatim/10df42c0-a10a-458f-a53f-73bdce906408/scratchpad/facture.xml"), invoiceService.xml(invoice.id(), librarian));
		assertThat(text).contains("Facture " + invoice.numero(), "41,32 EUR", "8,68 EUR", "50,00 EUR", "0987.654.394", "sans valeur legale");
	}

	private ValidationResultList validatePeppol(String xml) throws Exception {
		ValidationExecutorSetRegistry<IValidationSourceXML> registry = new ValidationExecutorSetRegistry<>();
		PeppolValidation.initStandard(registry);
		DVRCoordinate rules = PeppolValidation2026_11.VID_OPENPEPPOL_INVOICE_UBL_V3;
		IValidationExecutorSet<IValidationSourceXML> executors = registry.getOfID(rules);
		assertThat(executors).as("regles Peppol " + rules).isNotNull();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		return ValidationExecutionManager.executeValidation(IValidityDeterminator.createDefault(), executors,
				ValidationSourceXML.create("facture.xml", document));
	}

	private void pay(UserEntity user, String reference, String amount) {
		packRepository.save(new CreditPackEntity(user.getInstitution(), 100, new BigDecimal(amount), reference, CreditPackStatus.EN_ATTENTE));
		creditService.confirmStripePayment(new StripeWebhookRequest(reference, "checkout.session.completed"));
	}

	private void completeBilling(InstitutionEntity institution) {
		institution.updateBilling("Universite de Demonstration", "0987654394", "Avenue des Tests 5", "1050", "Ixelles", "BE",
				"comptabilite@institution-a.example");
		institutionRepository.save(institution);
	}

	private InstitutionBillingRequest billing(String enterpriseNumber) {
		return new InstitutionBillingRequest("Universite de Demonstration", enterpriseNumber, "Avenue des Tests 5", "1050", "Ixelles", "BE",
				"comptabilite@institution-a.example");
	}

	private UserEntity saveUser(String code, String domain, String email, UserRole role) {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity(code, "Institution " + code, domain));
		return userRepository.save(new UserEntity("Sarah", "Lemaire", email, passwordService.hash("558435"), role, institution));
	}
}

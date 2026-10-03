package be.icc.metamind.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.CreditPackRepository;
import be.icc.metamind.document.CreditPackStatus;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.PasswordService;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class CreditServiceTests {
	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private CreditService creditService;

	@Autowired
	private CreditMovementRepository movementRepository;

	@Autowired
	private CreditPackRepository packRepository;

	@Autowired
	private StripeProcessedEventRepository stripeProcessedEventRepository;

	@Test
	void freePackIsNotAvailable() {
		UserEntity user = saveUser();

		assertThatThrownBy(() -> creditService.startCheckout(user, new CreditCheckoutRequest(1, true, true)))
				.isInstanceOf(ApiException.class)
				.hasMessage("Le pack de credits est introuvable.");
	}

	@Test
	void webhookConfirmationIsIdempotent() {
		UserEntity user = saveUser();
		String reference = "pay_test";
		packRepository.save(new be.icc.metamind.document.CreditPackEntity(
				user.getInstitution(), 100, new BigDecimal("50.00"), reference, CreditPackStatus.EN_ATTENTE));

		creditService.confirmStripePayment(new StripeWebhookRequest(reference, "checkout.session.completed"));
		creditService.confirmStripePayment(new StripeWebhookRequest(reference, "checkout.session.completed"));

		assertThat(creditService.getBalance(user.getId()).balance()).isEqualTo(100);
		assertThat(movementRepository.count()).isEqualTo(1);
	}

	@Test
	void sameStripeEventIdIsStoredAndIgnored() {
		UserEntity user = saveUser();
		String reference = "pay_event";
		packRepository.save(new be.icc.metamind.document.CreditPackEntity(
				user.getInstitution(), 100, new BigDecimal("50.00"), reference, CreditPackStatus.EN_ATTENTE));

		StripeWebhookRequest event = new StripeWebhookRequest(
				reference, "checkout.session.completed", "evt_123", 5000L, "eur");
		creditService.confirmStripePayment(event);
		creditService.confirmStripePayment(event);

		assertThat(creditService.getBalance(user.getId()).balance()).isEqualTo(100);
		assertThat(stripeProcessedEventRepository.count()).isEqualTo(1);
	}

	@Test
	void webhookWithoutCompletedEventTypeDoesNotCreditAccount() {
		UserEntity user = saveUser();
		String reference = "pay_pending";
		packRepository.save(new be.icc.metamind.document.CreditPackEntity(
				user.getInstitution(), 100, new BigDecimal("50.00"), reference, CreditPackStatus.EN_ATTENTE));

		creditService.confirmStripePayment(new StripeWebhookRequest(reference, null));

		assertThat(creditService.getBalance(user.getId()).balance()).isZero();
		assertThat(movementRepository.count()).isZero();
	}

	@Test
	void unknownPackIsRejected() {
		UserEntity user = saveUser();

		assertThatThrownBy(() -> creditService.startCheckout(user, new CreditCheckoutRequest(99, true, true)))
				.isInstanceOf(ApiException.class)
				.hasMessage("Le pack de credits est introuvable.");
	}

	@Test
	void paidCheckoutRequiresStripeConfiguration() {
		UserEntity user = saveUser();

		assertThatThrownBy(() -> creditService.startCheckout(user, new CreditCheckoutRequest(2, true, true)))
				.isInstanceOf(ApiException.class)
				.hasMessage("Le role gestionnaire financier est requis pour acheter des credits.");
		assertThat(creditService.getBalance(user.getId()).balance()).isZero();
	}

	@Test
	void financialManagerCanReachStripeConfigurationCheck() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-F", "Institution F", "institution-f.example"));
		UserEntity user = userRepository.save(new UserEntity(
				"Finance", "Manager", "finance@institution-f.example", passwordService.hash("558435"),
				UserRole.GESTIONNAIRE_FINANCIER, institution));

		assertThatThrownBy(() -> creditService.startCheckout(user, new CreditCheckoutRequest(2, true, true)))
				.isInstanceOf(ApiException.class)
				.hasMessage("Le paiement Stripe n'est pas configure.");
	}

	private UserEntity saveUser() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		return userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("558435"),
				UserRole.LIBRARIAN,
				institution
		));
	}
}

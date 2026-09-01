package be.icc.metamind.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	@Test
	void creditsAreAttachedToUserInstitution() {
		UserEntity user = saveUser();

		CreditBalanceResponse balance = creditService.purchase(user.getId(), new CreditPurchaseRequest(25));

		assertThat(balance.balance()).isEqualTo(25);
		assertThat(creditService.getBalance(user.getId()).balance()).isEqualTo(25);
		assertThat(creditService.listMovements(user.getId()))
				.hasSize(1)
				.first()
				.satisfies(movement -> {
					assertThat(movement.type()).isEqualTo(CreditMovementType.ACHAT);
					assertThat(movement.amount()).isEqualTo(25);
					assertThat(movement.balanceAfter()).isEqualTo(25);
				});
		assertThat(movementRepository.count()).isEqualTo(1);
	}

	@Test
	void checkoutWaitsForPaymentConfirmationBeforeCreditingInstitution() {
		UserEntity user = saveUser();

		CreditCheckoutResponse checkout = creditService.startCheckout(user, new CreditCheckoutRequest(2, true));

		assertThat(checkout.reference()).startsWith("pay_");
		assertThat(checkout.checkoutUrl()).contains(checkout.reference());
		assertThat(creditService.getBalance(user.getId()).balance()).isZero();
		assertThat(movementRepository.count()).isZero();

		CreditBalanceResponse confirmed = creditService.confirmStripePayment(new StripeWebhookRequest(checkout.reference(), "checkout.session.completed"));

		assertThat(confirmed.balance()).isEqualTo(100);
		assertThat(movementRepository.count()).isEqualTo(1);
		assertThat(packRepository.findByPaymentReference(checkout.reference()).orElseThrow().getStatus()).isEqualTo(CreditPackStatus.PAYE);
	}

	@Test
	void webhookConfirmationIsIdempotent() {
		UserEntity user = saveUser();
		CreditCheckoutResponse checkout = creditService.startCheckout(user, new CreditCheckoutRequest(3, true));

		creditService.confirmStripePayment(new StripeWebhookRequest(checkout.reference(), "checkout.session.completed"));
		creditService.confirmStripePayment(new StripeWebhookRequest(checkout.reference(), "checkout.session.completed"));

		assertThat(creditService.getBalance(user.getId()).balance()).isEqualTo(500);
		assertThat(movementRepository.count()).isEqualTo(1);
	}

	@Test
	void unknownPackIsRejected() {
		UserEntity user = saveUser();

		assertThatThrownBy(() -> creditService.startCheckout(user, new CreditCheckoutRequest(99, true)))
				.isInstanceOf(ApiException.class)
				.hasMessage("Le pack de credits est introuvable.");
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

package be.icc.metamind.credit;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test
	void creditsAreAttachedToUserInstitution() {
		UserEntity user = saveUser();

		CreditBalanceResponse balance = creditService.purchase(user.getId(), new CreditPurchaseRequest(25));

		assertThat(balance.balance()).isEqualTo(25);
		assertThat(creditService.getBalance(user.getId()).balance()).isEqualTo(25);
	}

	private UserEntity saveUser() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		return userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("MotDePasse123"),
				UserRole.BIBLIOTHECAIRE,
				institution
		));
	}
}

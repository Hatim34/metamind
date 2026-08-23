package be.icc.metamind.user;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class UserRepositoryTests {
	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsUserByEmail() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		UserEntity user = new UserEntity("Sarah", "Lemaire", "sarah@institution-a.example", "hash", UserRole.BIBLIOTHECAIRE, institution);

		userRepository.save(user);

		assertThat(userRepository.findByEmailIgnoreCase("SARAH@INSTITUTION-A.EXAMPLE"))
				.isPresent()
				.get()
				.extracting(UserEntity::getFirstName)
				.isEqualTo("Sarah");
	}

	@Test
	void checksExistingEmail() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-B", "Institution B", "institution-b.example"));
		userRepository.save(new UserEntity("Jan", "Peeters", "jan@institution-b.example", "hash", UserRole.BIBLIOTHECAIRE, institution));

		assertThat(userRepository.existsByEmailIgnoreCase("JAN@INSTITUTION-B.EXAMPLE")).isTrue();
		assertThat(userRepository.existsByEmailIgnoreCase("autre@institution-b.example")).isFalse();
	}

	@Test
	void supportsLogicalDeactivation() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-C", "Institution C", "institution-c.example"));
		UserEntity user = userRepository.save(new UserEntity("Mina", "Laurent", "mina@institution-c.example", "hash", UserRole.BIBLIOTHECAIRE, institution));

		user.deactivate();
		userRepository.flush();

		assertThat(userRepository.findById(user.getId()))
				.isPresent()
				.get()
				.extracting(UserEntity::getStatus)
				.isEqualTo(UserStatus.DESACTIVE);
	}
}

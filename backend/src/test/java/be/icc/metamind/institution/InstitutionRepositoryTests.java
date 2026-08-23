package be.icc.metamind.institution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class InstitutionRepositoryTests {
	@Autowired
	private InstitutionRepository repository;

	@Test
	void savesAndFindsInstitutionByCode() {
		InstitutionEntity institution = new InstitutionEntity("INST-A", "Institution A", "institution-a.example");
		repository.save(institution);

		assertThat(repository.findByCodeIgnoreCase("inst-a"))
				.isPresent()
				.get()
				.extracting(InstitutionEntity::getName)
				.isEqualTo("Institution A");
	}

	@Test
	void checksInstitutionEmailDomain() {
		repository.save(new InstitutionEntity("INST-B", "Institution B", "institution-b.example"));

		assertThat(repository.existsByEmailDomainIgnoreCase("INSTITUTION-B.EXAMPLE")).isTrue();
		assertThat(repository.existsByEmailDomainIgnoreCase("autre.example")).isFalse();
	}

	@Test
	void supportsLogicalDeactivation() {
		InstitutionEntity institution = repository.save(new InstitutionEntity("INST-C", "Institution C", "institution-c.example"));

		institution.deactivate();
		repository.flush();

		assertThat(repository.findById(institution.getId()))
				.isPresent()
				.get()
				.extracting(InstitutionEntity::isActive)
				.isEqualTo(false);
	}
}

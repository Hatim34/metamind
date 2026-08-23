package be.icc.metamind.publication;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class PublicationRepositoryTests {
	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private PublicationRepository publicationRepository;

	@Test
	void savesPublicationLinkedToInstitution() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		PublicationEntity publication = new PublicationEntity(
				"Indexation multilingue de publications scientifiques",
				"Mina Laurent",
				2024,
				PublicationStatus.PUBLIE,
				Visibility.PUBLIC,
				"indexation,recherche,multilingue",
				institution
		);

		publicationRepository.save(publication);

		assertThat(publicationRepository.findById(publication.getId()))
				.isPresent()
				.get()
				.extracting(found -> found.getInstitution().getName())
				.isEqualTo("Institution A");
	}

	@Test
	void searchesPublicationByKeyword() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-B", "Institution B", "institution-b.example"));
		publicationRepository.save(new PublicationEntity(
				"Validation humaine des suggestions",
				"Jan Peeters",
				2025,
				PublicationStatus.A_VALIDER,
				Visibility.INSTITUTION,
				"validation,catalogage,qualite",
				institution
		));

		assertThat(publicationRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrKeywordsTextContainingIgnoreCase("catalogage", "catalogage", "catalogage"))
				.hasSize(1)
				.first()
				.extracting(PublicationEntity::getStatus)
				.isEqualTo(PublicationStatus.A_VALIDER);
	}
}

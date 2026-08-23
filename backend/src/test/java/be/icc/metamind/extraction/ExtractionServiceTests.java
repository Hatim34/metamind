package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;
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
class ExtractionServiceTests {
	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublicationRepository publicationRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private ExtractionService extractionService;

	@Test
	void extractionConsumesOneCreditAndUpdatesPublication() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		institution.addCredits(2);
		UserEntity user = userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("558435"),
				UserRole.BIBLIOTHECAIRE,
				institution
		));
		PublicationEntity publication = publicationRepository.save(new PublicationEntity(
				"Analyse automatique des metadonnees pour les depots institutionnels",
				"Sarah Lemaire",
				2026,
				PublicationStatus.EN_ATTENTE,
				Visibility.PUBLIC,
				"",
				institution
		));

		MetadataExtractionResponse response = extractionService.extract(publication.getId(), user.getId());

		assertThat(response.creditBalance()).isEqualTo(1);
		assertThat(response.suggestedKeywords()).contains("Dublin Core");
		assertThat(publication.getStatus()).isEqualTo(PublicationStatus.A_VALIDER);
	}
}

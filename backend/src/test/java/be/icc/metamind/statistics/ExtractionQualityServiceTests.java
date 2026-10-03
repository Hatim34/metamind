package be.icc.metamind.statistics;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataAuthorRequest;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataService;
import be.icc.metamind.document.MetadataValidationRequest;
import be.icc.metamind.extraction.ExtractionService;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.support.TestDocumentFactory;
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
class ExtractionQualityServiceTests {
	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private MetadataRepository metadataRepository;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private DocumentAuthorRepository documentAuthorRepository;

	@Autowired
	private DocumentKeywordRepository documentKeywordRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private ExtractionService extractionService;

	@Autowired
	private MetadataService metadataService;

	@Autowired
	private ExtractionQualityService qualityService;

	@Test
	void reportsNothingWhenNoSuggestionHasBeenArbitratedYet() {
		UserEntity user = librarian("INST-A", "institution-a.example");

		ExtractionQualityResponse quality = qualityService.getQuality(user);

		assertThat(quality.arbitratedSuggestions()).isZero();
		assertThat(quality.overallAcceptanceRate()).isZero();
		assertThat(quality.byField()).isEmpty();
		// Les tranches de calibration restent presentes mais vides : le tableau de bord garde sa forme.
		assertThat(quality.calibration()).hasSize(3).allSatisfy(bucket ->
				assertThat(bucket.arbitrated()).isZero());
	}

	@Test
	void measuresAcceptanceAndCorrectionPerField() {
		UserEntity user = librarian("INST-A", "institution-a.example");
		validateWithTitle(user, "Analyse automatique des metadonnees", "analyse automatique des metadonnees");
		validateWithTitle(user, "Catalogage assiste des theses", "Un titre entierement reecrit par le bibliothecaire");

		ExtractionQualityResponse quality = qualityService.getQuality(user);

		assertThat(quality.scope()).isEqualTo("Institution A");
		assertThat(quality.byField())
				.filteredOn(field -> field.field().equals("titre"))
				.singleElement()
				.satisfies(field -> {
					assertThat(field.arbitrated()).isEqualTo(2);
					assertThat(field.accepted()).isEqualTo(1);
					assertThat(field.modified()).isEqualTo(1);
					assertThat(field.acceptanceRate()).isEqualTo(50.0);
					assertThat(field.averageEditDistance()).isGreaterThan(0.0);
				});
	}

	@Test
	void neverExposesTheArbitrationsOfAnotherInstitution() {
		UserEntity first = librarian("INST-A", "institution-a.example");
		UserEntity second = librarian("INST-B", "institution-b.example");
		validateWithTitle(first, "Analyse automatique des metadonnees", "analyse automatique des metadonnees");

		assertThat(qualityService.getQuality(first).arbitratedSuggestions()).isPositive();
		assertThat(qualityService.getQuality(second).arbitratedSuggestions()).isZero();
	}

	private void validateWithTitle(UserEntity user, String documentTitle, String publishedTitle) {
		DocumentEntity document = new TestDocumentFactory(
				documentRepository, metadataRepository, authorRepository,
				keywordRepository, documentAuthorRepository, documentKeywordRepository
		).create(
				documentTitle,
				user.getFirstName() + " " + user.getLastName(),
				2026,
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.PUBLIC,
				List.of(),
				user.getInstitution(),
				user
		);
		extractionService.extract(document.getId(), user);
		metadataService.validateMetadata(document.getId(), new MetadataValidationRequest(
				publishedTitle,
				"Resume valide par le bibliothecaire.",
				LocalDate.of(2026, 1, 1),
				"Sciences de l'information",
				DocumentVisibility.PUBLIC,
				List.of(new MetadataAuthorRequest(user.getFirstName() + " " + user.getLastName(), null)),
				List.of("metadonnees", "Dublin Core", "catalogage"),
				null,
				null,
				null
		), user);
	}

	private UserEntity librarian(String institutionCode, String domain) {
		InstitutionEntity institution = institutionRepository.save(
				new InstitutionEntity(institutionCode, "Institution " + institutionCode.substring(5), domain));
		institution.addCredits(10);
		return userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@" + domain,
				passwordService.hash("558435"),
				UserRole.LIBRARIAN,
				institution
		));
	}
}

package be.icc.metamind.document;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.extraction.ExtractionService;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.support.TestDocumentFactory;
import be.icc.metamind.user.PasswordService;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * La langue, le type de document et le DOI sont desormais arbitres par le bibliothecaire
 * comme les autres champs, en restant dans les vocabulaires de reference.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class MetadataReferenceValidationTests {
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
	private LanguageRepository languageRepository;

	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	@Autowired
	private MetadataSuggestionRepository suggestionRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private ExtractionService extractionService;

	@Autowired
	private MetadataService metadataService;

	@BeforeEach
	void seedReferenceData() {
		if (languageRepository.findByCodeIgnoreCase("fr").isEmpty()) {
			languageRepository.save(new LanguageEntity("fr", "Francais"));
		}
		if (documentTypeRepository.findByCodeIgnoreCase("these").isEmpty()) {
			documentTypeRepository.save(new DocumentTypeEntity("these", "These"));
		}
	}

	@Test
	void storesTheReferencesChosenByTheLibrarian() {
		Fixture fixture = extractedDocument();

		MetadataResponse response = metadataService.validateMetadata(
				fixture.documentId(), request("fr", "these", "10.1234/reel.2026"), fixture.user());

		assertThat(response.language()).isEqualTo("fr");
		assertThat(response.documentType()).isEqualTo("these");
		assertThat(response.doi()).isEqualTo("10.1234/reel.2026");
	}

	@Test
	void recordsTheArbitrationOfTheReferenceFields() {
		Fixture fixture = extractedDocument();

		metadataService.validateMetadata(
				fixture.documentId(), request("fr", "these", null), fixture.user());

		// Aucune langue detectee sur ce document court : la renseigner est une correction.
		assertThat(decisionOf(fixture, "langue")).isEqualTo(SuggestionDecision.MODIFIE);
		// Aucun type n'avait ete detecte sur ce document : en choisir un est une correction.
		assertThat(decisionOf(fixture, "type_document")).isEqualTo(SuggestionDecision.MODIFIE);
	}

	@Test
	void refusesALanguageOutsideTheReferenceVocabulary() {
		Fixture fixture = extractedDocument();

		assertThatThrownBy(() -> metadataService.validateMetadata(
				fixture.documentId(), request("klingon", null, null), fixture.user()))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("klingon");
	}

	@Test
	void refusesADocumentTypeOutsideTheReferenceVocabulary() {
		Fixture fixture = extractedDocument();

		assertThatThrownBy(() -> metadataService.validateMetadata(
				fixture.documentId(), request("fr", "poeme", null), fixture.user()))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("poeme");
	}

	private SuggestionDecision decisionOf(Fixture fixture, String field) {
		return suggestionRepository.findAll().stream()
				.filter(suggestion -> suggestion.getEnrichment().getDocument().getId().equals(fixture.documentId()))
				.filter(suggestion -> suggestion.getChamp().equals(field))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Aucune suggestion pour le champ " + field))
				.getDecision();
	}

	private MetadataValidationRequest request(String language, String documentType, String doi) {
		return new MetadataValidationRequest(
				"analyse automatique des metadonnees",
				"Resume valide par le bibliothecaire.",
				LocalDate.of(2026, 1, 1),
				"Sciences de l'information",
				DocumentVisibility.PUBLIC,
				List.of(new MetadataAuthorRequest("Sarah Lemaire", null)),
				List.of("metadonnees", "Dublin Core", "catalogage"),
				language,
				documentType,
				doi
		);
	}

	private Fixture extractedDocument() {
		InstitutionEntity institution = institutionRepository.save(
				new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		institution.addCredits(5);
		UserEntity user = userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("558435"),
				UserRole.LIBRARIAN,
				institution
		));
		DocumentEntity document = new TestDocumentFactory(
				documentRepository, metadataRepository, authorRepository,
				keywordRepository, documentAuthorRepository, documentKeywordRepository
		).create(
				"Analyse automatique des metadonnees",
				"Sarah Lemaire",
				2026,
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.PUBLIC,
				List.of(),
				institution,
				user
		);
		extractionService.extract(document.getId(), user);
		return new Fixture(document.getId(), user);
	}

	private record Fixture(long documentId, UserEntity user) {
	}
}

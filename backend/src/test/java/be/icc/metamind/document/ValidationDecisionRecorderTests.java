package be.icc.metamind.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * Verifie que l'arbitrage du bibliothecaire est bien trace champ par champ.
 * Ces traces sont la source unique des indicateurs de fiabilite du LLM.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class ValidationDecisionRecorderTests {
	/** Titre que l'extracteur local deduit du nom de fichier genere par la fabrique de test. */
	private static final String DOCUMENT_TITLE = "Analyse automatique des metadonnees";
	private static final String SUGGESTED_TITLE = "analyse automatique des metadonnees";
	private static final String SUGGESTED_CLASSIFICATION = "Sciences de l'information";

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
	private MetadataSuggestionRepository suggestionRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private ExtractionService extractionService;

	@Autowired
	private MetadataService metadataService;

	@Test
	void anUntouchedSuggestionIsRecordedAsAccepted() {
		Fixture fixture = extractedDocument();

		metadataService.validateMetadata(fixture.documentId(), request(
				SUGGESTED_TITLE,
				SUGGESTED_CLASSIFICATION,
				List.of("Sarah Lemaire"),
				List.of("metadonnees", "Dublin Core", "catalogage")
		), fixture.user());

		assertDecision(fixture, "titre", suggestion -> {
			assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.ACCEPTE);
			assertThat(suggestion.getFinalValue()).isEqualTo(SUGGESTED_TITLE);
			assertThat(suggestion.getEditDistance()).isEqualByComparingTo("0.000");
		});
		assertDecision(fixture, "classification", suggestion ->
				assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.ACCEPTE));
		assertDecision(fixture, "auteurs", suggestion ->
				assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.ACCEPTE));
	}

	@Test
	void aCorrectedSuggestionIsRecordedAsModifiedWithItsEditDistance() {
		Fixture fixture = extractedDocument();

		metadataService.validateMetadata(fixture.documentId(), request(
				"Extraction automatique de metadonnees par un modele de langue",
				SUGGESTED_CLASSIFICATION,
				List.of("Sarah Lemaire"),
				List.of("metadonnees", "Dublin Core", "catalogage")
		), fixture.user());

		assertDecision(fixture, "titre", suggestion -> {
			assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.MODIFIE);
			assertThat(suggestion.getEditDistance()).isGreaterThan(BigDecimal.ZERO);
			assertThat(suggestion.getFinalValue()).isEqualTo("Extraction automatique de metadonnees par un modele de langue");
		});
	}

	@Test
	void anErasedSuggestionIsRecordedAsEmptied() {
		Fixture fixture = extractedDocument();

		metadataService.validateMetadata(fixture.documentId(), request(
				SUGGESTED_TITLE,
				null,
				List.of("Sarah Lemaire"),
				List.of("metadonnees", "Dublin Core", "catalogage")
		), fixture.user());

		assertDecision(fixture, "classification", suggestion -> {
			assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.VIDE);
			assertThat(suggestion.getFinalValue()).isEmpty();
		});
	}

	@Test
	void reorderingAListIsNotCountedAsACorrection() {
		Fixture fixture = extractedDocument();

		metadataService.validateMetadata(fixture.documentId(), request(
				SUGGESTED_TITLE,
				SUGGESTED_CLASSIFICATION,
				List.of("Sarah Lemaire"),
				List.of("catalogage", "metadonnees", "Dublin Core")
		), fixture.user());

		assertDecision(fixture, "mots_cles", suggestion ->
				assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.ACCEPTE));
	}

	@Test
	void rejectingTheMetadataMarksEverySuggestionAsRejected() {
		Fixture fixture = extractedDocument();

		metadataService.rejectMetadata(
				fixture.documentId(),
				new MetadataRejectionRequest("Metadonnees incoherentes avec le document."),
				fixture.user());

		assertThat(suggestionsOf(fixture))
				.isNotEmpty()
				.allSatisfy(suggestion -> {
					assertThat(suggestion.getDecision()).isEqualTo(SuggestionDecision.REJETE);
					assertThat(suggestion.getFinalValue()).isNull();
				});
	}

	private MetadataValidationRequest request(String title, String classification, List<String> authors, List<String> keywords) {
		return new MetadataValidationRequest(
				title,
				"Resume valide par le bibliothecaire.",
				LocalDate.of(2026, 1, 1),
				classification,
				DocumentVisibility.PUBLIC,
				authors.stream().map(name -> new MetadataAuthorRequest(name, null)).toList(),
				keywords,
				null,
				null,
				null
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
				DOCUMENT_TITLE,
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

	private List<MetadataSuggestionEntity> suggestionsOf(Fixture fixture) {
		return suggestionRepository.findAll().stream()
				.filter(suggestion -> suggestion.getEnrichment().getDocument().getId().equals(fixture.documentId()))
				.toList();
	}

	private void assertDecision(Fixture fixture, String field, ThrowingConsumer assertions) {
		MetadataSuggestionEntity suggestion = suggestionsOf(fixture).stream()
				.filter(byField(field))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Aucune suggestion pour le champ " + field));
		assertions.accept(suggestion);
	}

	private Predicate<MetadataSuggestionEntity> byField(String field) {
		return suggestion -> suggestion.getChamp().equals(field);
	}

	private interface ThrowingConsumer {
		void accept(MetadataSuggestionEntity suggestion);
	}

	private record Fixture(long documentId, UserEntity user) {
	}
}

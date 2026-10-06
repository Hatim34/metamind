package be.icc.metamind.extraction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentTypeEntity;
import be.icc.metamind.document.DocumentTypeRepository;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.EnrichmentEntity;
import be.icc.metamind.document.EnrichmentRepository;
import be.icc.metamind.document.EnrichmentStatus;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.LanguageEntity;
import be.icc.metamind.document.LanguageRepository;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataSuggestionRepository;
import be.icc.metamind.document.MetadataSuggestionSource;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;
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
class ExtractionServiceTests {
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
	private CreditMovementRepository movementRepository;

	@Autowired
	private EnrichmentRepository enrichmentRepository;

	@Autowired
	private MetadataSuggestionRepository suggestionRepository;

	@Autowired
	private LanguageRepository languageRepository;

	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	@Test
	void extractionConsumesOneCreditAndUpdatesPublication() {
		languageRepository.save(new LanguageEntity("fr", "Francais"));
		documentTypeRepository.save(new DocumentTypeEntity("autre", "Autre"));
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		institution.addCredits(2);
		UserEntity user = userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("558435"),
				UserRole.LIBRARIAN,
				institution
		));
		DocumentEntity publication = new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository).create(
				"Analyse automatique des metadonnees pour les depots institutionnels",
				"Sarah Lemaire",
				2026,
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.PUBLIC,
				List.of(),
				institution,
				user
		);

		MetadataExtractionResponse response = extractionService.extract(publication.getId(), user);

		assertThat(response.creditBalance()).isEqualTo(1);
		assertThat(response.suggestedKeywords()).contains("Dublin Core");
		assertThat(publication.getStatus()).isEqualTo(DocumentStatus.A_VALIDER);
		MetadataEntity metadata = metadataRepository.findByDocumentId(publication.getId()).orElseThrow();
		assertThat(metadata.getTitre()).isEqualTo("analyse automatique des metadonnees pour les depots institutionnels");
		assertThat(metadata.getResume()).contains("Analyse automatique des metadonnees");
		assertThat(metadata.getClassification()).isEqualTo("Sciences de l'information");
		// Document de test trop court pour conclure : la langue reste vide plutot
		// que d'etre devinee. Un vrai PDF fournit assez de texte pour la detecter.
		assertThat(metadata.getLanguage()).isNull();
		// Aucun indice de type dans ce document : le type reste vide au lieu d'etre invente.
		assertThat(metadata.getDocumentType()).isNull();
		EnrichmentEntity enrichment = enrichmentRepository.findAll().stream()
				.filter(item -> item.getDocument().getId().equals(publication.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(enrichment.getStatus()).isEqualTo(EnrichmentStatus.TERMINE);
		assertThat(suggestionRepository.findAll())
				.filteredOn(suggestion -> suggestion.getEnrichment().getId().equals(enrichment.getId()))
				.extracting("champ")
				.containsExactlyInAnyOrder(
						"titre", "auteurs", "resume", "classification", "mots_cles",
						"langue", "type_document", "date_publication");
		assertThat(suggestionRepository.findAll())
				.filteredOn(suggestion -> suggestion.getEnrichment().getId().equals(enrichment.getId()))
				.extracting(suggestion -> suggestion.getSource())
				.containsOnly(MetadataSuggestionSource.LLM);
		assertThat(suggestionRepository.findAll())
				.filteredOn(suggestion -> suggestion.getEnrichment().getId().equals(enrichment.getId()))
				.filteredOn(suggestion -> suggestion.getChamp().equals("titre"))
				.singleElement()
				.satisfies(suggestion -> {
					assertThat(suggestion.getEvidence()).contains("Analyse automatique des metadonnees");
					assertThat(suggestion.getSignals()).contains("preuve retrouvee");
					assertThat(suggestion.getSegments()).isEqualTo("S1");
				});
		assertThat(movementRepository.findByInstitutionIdOrderByCreatedAtDesc(institution.getId()))
				.hasSize(1)
				.first()
				.satisfies(movement -> {
					assertThat(movement.getType()).isEqualTo(CreditMovementType.CONSOMMATION);
					assertThat(movement.getAmount()).isEqualTo(-1);
					assertThat(movement.getBalanceAfter()).isEqualTo(1);
				});
	}
}

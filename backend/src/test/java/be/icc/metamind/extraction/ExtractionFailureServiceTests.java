package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.EnrichmentRepository;
import be.icc.metamind.document.EnrichmentStatus;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"metamind.llm.provider=failing"
})
@Transactional
class ExtractionFailureServiceTests {
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

	@Test
	void failedExtractionDoesNotConsumeCredit() {
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
		DocumentEntity document = new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository).create(
				"Analyse automatique des metadonnees",
				"Sarah Lemaire",
				2026,
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.PUBLIC,
				List.of(),
				institution,
				user
		);

		assertThatThrownBy(() -> extractionService.extract(document.getId(), user))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("indisponible");

		assertThat(institution.getCreditBalance()).isEqualTo(2);
		assertThat(document.getStatus()).isEqualTo(DocumentStatus.EN_ATTENTE);
		assertThat(movementRepository.findByInstitutionIdOrderByCreatedAtDesc(institution.getId())).isEmpty();
		assertThat(enrichmentRepository.findAll())
				.filteredOn(enrichment -> enrichment.getDocument().getId().equals(document.getId()))
				.singleElement()
				.satisfies(enrichment -> {
					assertThat(enrichment.getStatus()).isEqualTo(EnrichmentStatus.ECHEC);
					assertThat(enrichment.getErrorMessage()).contains("indisponible");
				});
	}

	@TestConfiguration
	static class FailingProviderConfiguration {
		@Bean
		MetadataExtractionProvider metadataExtractionProvider() {
			return new MetadataExtractionProvider() {
				@Override
				public MetadataExtractionData extract(DocumentEntity document) {
					throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Fournisseur d'extraction indisponible.");
				}

				@Override
				public String modelName() {
					return "local-unavailable";
				}
			};
		}
	}
}

package be.icc.metamind.extraction;

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
import be.icc.metamind.document.MetadataRepository;
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

	@Test
	void extractionConsumesOneCreditAndUpdatesPublication() {
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

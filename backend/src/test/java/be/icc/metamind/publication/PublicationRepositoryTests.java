package be.icc.metamind.publication;

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
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.support.TestDocumentFactory;

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

	@Test
	void savesPublicationLinkedToInstitution() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		DocumentEntity document = factory().create(
				"Indexation multilingue de publications scientifiques",
				"Mina Laurent",
				2024,
				DocumentStatus.PUBLIE,
				DocumentVisibility.PUBLIC,
				List.of("indexation", "recherche", "multilingue"),
				institution,
				null
		);

		assertThat(documentRepository.findById(document.getId()))
				.isPresent()
				.get()
				.extracting(found -> found.getInstitution().getName())
				.isEqualTo("Institution A");
	}

	@Test
	void searchesPublicationByKeyword() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-B", "Institution B", "institution-b.example"));
		factory().create(
				"Validation humaine des suggestions",
				"Jan Peeters",
				2025,
				DocumentStatus.A_VALIDER,
				DocumentVisibility.INSTITUTION,
				List.of("validation", "catalogage", "qualite"),
				institution,
				null
		);

		assertThat(documentRepository.search("catalogage"))
				.hasSize(1)
				.first()
				.extracting(DocumentEntity::getStatus)
				.isEqualTo(DocumentStatus.A_VALIDER);
	}

	private TestDocumentFactory factory() {
		return new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository);
	}
}

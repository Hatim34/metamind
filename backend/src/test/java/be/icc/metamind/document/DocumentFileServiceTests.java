package be.icc.metamind.document;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.support.TestDocumentFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les fichiers importes avant leur conservation en base ont disparu du disque de
 * production : ils sont rattaches a leur document grace au nom enregistre a l'import.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class DocumentFileServiceTests {
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

	@Autowired
	private DocumentFileService documentFileService;

	@Test
	void restoresTheLostFileOfTheDocumentImportedUnderThatName() {
		DocumentEntity document = document("Gestion des fonds patrimoniaux");
		MockMultipartFile upload = new MockMultipartFile(
				"fichier", "gestion-des-fonds-patrimoniaux.txt", "text/plain", "Texte du document".getBytes(StandardCharsets.UTF_8));

		List<Long> restored = documentFileService.restoreMissingFiles(upload);

		assertThat(restored).containsExactly(document.getId());
		assertThat(documentFileService.load(document.getId()))
				.get()
				.extracting(file -> new String(file.content(), StandardCharsets.UTF_8))
				.isEqualTo("Texte du document");
	}

	@Test
	void neverReplacesAFileAlreadyKeptInTheDatabase() {
		DocumentEntity document = document("Numerisation des archives");
		MockMultipartFile first = new MockMultipartFile(
				"fichier", "numerisation-des-archives.txt", "text/plain", "Original".getBytes(StandardCharsets.UTF_8));
		MockMultipartFile second = new MockMultipartFile(
				"fichier", "numerisation-des-archives.txt", "text/plain", "Autre".getBytes(StandardCharsets.UTF_8));
		documentFileService.restoreMissingFiles(first);

		assertThat(documentFileService.restoreMissingFiles(second)).isEmpty();
		assertThat(documentFileService.load(document.getId()))
				.get()
				.extracting(file -> new String(file.content(), StandardCharsets.UTF_8))
				.isEqualTo("Original");
	}

	private DocumentEntity document(String title) {
		InstitutionEntity institution = institutionRepository.save(
				new InstitutionEntity("INST-F", "Institution F", "institution-f.example"));
		return new TestDocumentFactory(
				documentRepository, metadataRepository, authorRepository,
				keywordRepository, documentAuthorRepository, documentKeywordRepository
		).create(title, "Sarah Lemaire", 2025, DocumentStatus.A_VALIDER, DocumentVisibility.PUBLIC, List.of(), institution, null);
	}
}

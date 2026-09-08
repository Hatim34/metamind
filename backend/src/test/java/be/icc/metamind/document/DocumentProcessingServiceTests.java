package be.icc.metamind.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTests {
	@Mock
	private DocumentRepository documentRepository;

	@Mock
	private DocumentUploadService documentUploadService;

	@Test
	void processExtractsTextAndReturnsDocumentToWaitingStatus() {
		DocumentEntity document = new DocumentEntity(
				"article.pdf",
				"/tmp/article.pdf",
				100L,
				"PDF",
				null,
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.INSTITUTION,
				org.mockito.Mockito.mock(be.icc.metamind.institution.InstitutionEntity.class),
				null
		);
		when(documentRepository.findById(7L)).thenReturn(Optional.of(document));
		when(documentUploadService.extractText(any(Path.class))).thenReturn("Texte extrait");
		when(documentUploadService.storePdfThumbnail(any(Path.class))).thenReturn("/tmp/cover.jpg");

		new DocumentProcessingService(documentRepository, documentUploadService, null).process(7L, "/tmp/article.pdf");

		assertEquals(DocumentStatus.EN_ATTENTE, document.getStatus());
		assertEquals("Texte extrait", document.getExtractedText());
		assertEquals("/tmp/cover.jpg", document.getCoverImagePath());
		verify(documentRepository).findById(7L);
		verify(documentRepository).save(document);
	}
}

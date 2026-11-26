package be.icc.metamind.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
	void processExtractsTextAndLeavesTheDocumentWaitingForAnalysis() {
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
		when(documentRepository.markProcessingIfNotDeleted(7L)).thenReturn(1);
		when(documentRepository.completeProcessingIfNotDeleted(7L, "Texte extrait", "/tmp/cover.jpg")).thenReturn(1);
		when(documentUploadService.extractText(any(Path.class))).thenReturn("Texte extrait");
		when(documentUploadService.storePdfThumbnail(any(Path.class))).thenReturn("/tmp/cover.jpg");

		DocumentCoverService documentCoverService = org.mockito.Mockito.mock(DocumentCoverService.class);
		DocumentFileService documentFileService = org.mockito.Mockito.mock(DocumentFileService.class);

		new DocumentProcessingService(documentRepository, documentUploadService, documentCoverService, documentFileService, null)
				.process(7L, "/tmp/article.pdf");

		// Texte lu : le document attend l'analyse par l'IA, il n'est pas encore a valider.
		assertEquals(DocumentStatus.EN_ATTENTE, document.getStatus());
		assertNull(document.getExtractedText());
		assertNull(document.getCoverImagePath());
		verify(documentRepository).findById(7L);
		verify(documentRepository).markProcessingIfNotDeleted(7L);
		verify(documentRepository).completeProcessingIfNotDeleted(7L, "Texte extrait", "/tmp/cover.jpg");
		// La couverture doit etre recopiee en base : le disque n'est pas persistant en production.
		verify(documentCoverService).persistFromPath(document.getId(), "/tmp/cover.jpg");
		// Apres un redeploiement, le fichier est recree depuis la base avant d'etre relu.
		verify(documentFileService).restoreOnDisk(7L, "/tmp/article.pdf");
	}

	@Test
	void processDoesNotReviveASoftDeletedDocument() {
		DocumentEntity document = new DocumentEntity(
				"article.pdf", "/tmp/article.pdf", 100L, "PDF", null,
				DocumentStatus.SUPPRIME, DocumentVisibility.INSTITUTION,
				org.mockito.Mockito.mock(be.icc.metamind.institution.InstitutionEntity.class), null
		);
		when(documentRepository.findById(7L)).thenReturn(Optional.of(document));
		when(documentRepository.markProcessingIfNotDeleted(7L)).thenReturn(0);

		DocumentCoverService documentCoverService = org.mockito.Mockito.mock(DocumentCoverService.class);
		DocumentFileService documentFileService = org.mockito.Mockito.mock(DocumentFileService.class);

		new DocumentProcessingService(documentRepository, documentUploadService, documentCoverService, documentFileService, null)
				.process(7L, "/tmp/article.pdf");

		assertEquals(DocumentStatus.SUPPRIME, document.getStatus());
		verify(documentUploadService, never()).extractText(any(Path.class));
		verify(documentFileService, never()).restoreOnDisk(7L, "/tmp/article.pdf");
	}
}

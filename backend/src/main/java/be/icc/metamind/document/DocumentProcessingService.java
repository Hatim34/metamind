package be.icc.metamind.document;

import java.nio.file.Path;

import be.icc.metamind.api.ApiException;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DocumentProcessingService {
	private final DocumentRepository documentRepository;
	private final DocumentUploadService documentUploadService;
	private final DocumentCoverService documentCoverService;
	private final DocumentFileService documentFileService;
	private final DocumentProcessingService self;

	public DocumentProcessingService(DocumentRepository documentRepository, DocumentUploadService documentUploadService, DocumentCoverService documentCoverService, DocumentFileService documentFileService, @Lazy DocumentProcessingService self) {
		this.documentRepository = documentRepository;
		this.documentUploadService = documentUploadService;
		this.documentCoverService = documentCoverService;
		this.documentFileService = documentFileService;
		this.self = self;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processAfterImport(DocumentImportedEvent event) {
		self.process(event.documentId(), event.filePath());
	}

	@Async
	public void retry(long documentId, String filePath) {
		self.process(documentId, filePath);
	}

	@Transactional
	public void process(long documentId, String filePath) {
		DocumentEntity document = documentRepository.findById(documentId).orElse(null);
		if (document == null) {
			return;
		}
		if (documentRepository.markProcessingIfNotDeleted(documentId) == 0) {
			return;
		}
		try {
			// Apres un redeploiement, seul le fichier conserve en base existe encore.
			documentFileService.restoreOnDisk(documentId, filePath);
			String extractedText = documentUploadService.extractText(Path.of(filePath));
			String coverPath = document.getCoverImagePath();
			if (coverPath == null) {
				coverPath = documentUploadService.storePdfThumbnail(Path.of(filePath));
			}
			if (documentRepository.completeProcessingIfNotDeleted(documentId, extractedText, coverPath) > 0) {
				documentCoverService.persistFromPath(documentId, coverPath);
			}
		}
		catch (ApiException exception) {
			documentRepository.markFileUnreadableIfNotDeleted(documentId);
		}
		catch (RuntimeException exception) {
			documentRepository.markFileUnreadableIfNotDeleted(documentId);
		}
	}
}

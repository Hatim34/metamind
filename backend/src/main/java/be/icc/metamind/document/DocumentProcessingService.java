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
	private final DocumentProcessingService self;

	public DocumentProcessingService(DocumentRepository documentRepository, DocumentUploadService documentUploadService, @Lazy DocumentProcessingService self) {
		this.documentRepository = documentRepository;
		this.documentUploadService = documentUploadService;
		this.self = self;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processAfterImport(DocumentImportedEvent event) {
		self.process(event.documentId(), event.filePath());
	}

	@Transactional
	public void process(long documentId, String filePath) {
		DocumentEntity document = documentRepository.findById(documentId).orElse(null);
		if (document == null) {
			return;
		}
		document.markImportProcessing();
		try {
			String extractedText = documentUploadService.extractText(Path.of(filePath));
			String coverPath = document.getCoverImagePath();
			if (coverPath == null) {
				coverPath = documentUploadService.storePdfThumbnail(Path.of(filePath));
			}
			document.completeImportProcessing(extractedText, coverPath);
		}
		catch (ApiException exception) {
			document.markExtractionFailed();
		}
		catch (RuntimeException exception) {
			document.markExtractionFailed();
		}
		documentRepository.save(document);
	}
}

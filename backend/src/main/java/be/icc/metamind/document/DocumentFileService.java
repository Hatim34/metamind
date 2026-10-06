package be.icc.metamind.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentUploadService.StoredFile;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Conserve les fichiers importes en base, seul stockage durable en production.
 *
 * Le disque du conteneur sert au traitement juste apres l'import ; c'est la copie en
 * base qui est servie ensuite, et qui permet de recreer le fichier pour un retraitement.
 */
@Service
public class DocumentFileService {
	private final DocumentFileRepository fileRepository;
	private final DocumentRepository documentRepository;
	private final DocumentUploadService documentUploadService;

	public DocumentFileService(
			DocumentFileRepository fileRepository,
			DocumentRepository documentRepository,
			DocumentUploadService documentUploadService
	) {
		this.fileRepository = fileRepository;
		this.documentRepository = documentRepository;
		this.documentUploadService = documentUploadService;
	}

	/** Recopie en base le fichier present a ce chemin. Sans fichier lisible, rien n'est enregistre. */
	@Transactional
	public void persistFromPath(Long documentId, String filePath) {
		if (documentId == null) {
			return;
		}
		documentUploadService.readDocumentFileIfPresent(filePath)
				.filter(file -> file.content().length > 0)
				.ifPresent(file -> store(documentId, file.content(), file.mediaType()));
	}

	@Transactional(readOnly = true)
	public Optional<StoredFile> load(Long documentId) {
		if (documentId == null) {
			return Optional.empty();
		}
		return fileRepository.findById(documentId)
				.filter(file -> file.getData() != null && file.getData().length > 0)
				.map(file -> new StoredFile(file.getData(), MediaType.parseMediaType(file.getMediaType())));
	}

	/**
	 * Recree le fichier sur le disque a partir de la base s'il a disparu.
	 * Necessaire pour retraiter un document importe avant le dernier deploiement.
	 */
	@Transactional(readOnly = true)
	public void restoreOnDisk(Long documentId, String filePath) {
		if (filePath == null || filePath.isBlank() || Files.isRegularFile(Path.of(filePath))) {
			return;
		}
		load(documentId).ifPresent(file -> documentUploadService.writeDocumentFile(filePath, file.content()));
	}

	/**
	 * Rattache un fichier aux documents importes sous ce nom dont le fichier a ete perdu.
	 *
	 * Sert a reprendre les documents importes avant que les fichiers soient conserves en
	 * base : le nom enregistre a l'import identifie le document sans ambiguite.
	 */
	@Transactional
	public List<Long> restoreMissingFiles(MultipartFile upload) {
		String fileName = documentUploadService.validatedFileName(upload);
		byte[] content;
		try {
			content = upload.getBytes();
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le fichier n'a pas pu etre lu.");
		}
		MediaType mediaType = documentUploadService.documentMediaType(fileName);
		List<Long> restored = documentRepository.findIdsByFileName(fileName).stream()
				.filter(documentId -> !fileRepository.existsById(documentId))
				.toList();
		restored.forEach(documentId -> store(documentId, content, mediaType));
		return restored;
	}

	private void store(Long documentId, byte[] content, MediaType mediaType) {
		String type = mediaType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : mediaType.toString();
		fileRepository.findById(documentId)
				.map(existing -> {
					existing.replace(content, type);
					return existing;
				})
				.or(() -> Optional.of(new DocumentFileEntity(documentId, content, type)))
				.ifPresent(fileRepository::save);
	}
}

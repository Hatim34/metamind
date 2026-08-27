package be.icc.metamind.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import be.icc.metamind.api.ApiException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentUploadService {
	private static final long MAX_FILE_SIZE = 128L * 1024L * 1024L;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");
	private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
			"application/pdf",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"text/plain"
	);

	private final Path storageRoot;
	private final DocumentTextExtractor textExtractor;

	public DocumentUploadService(@Value("${metamind.storage.documents-dir:storage/documents}") String storageDirectory, DocumentTextExtractor textExtractor) {
		this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
		this.textExtractor = textExtractor;
	}

	public ImportedDocument importFile(MultipartFile file) {
		validate(file);
		String extractedText = textExtractor.extract(file);
		String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
		Path storedPath = store(file, originalFileName);
		return new ImportedDocument(originalFileName, storedPath.toString(), file.getSize(), normalizeMediaType(file), extractedText);
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le fichier est obligatoire.");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le fichier depasse la taille maximale de 128 MB.");
		}
		String extension = extension(file.getOriginalFilename());
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le format du fichier doit etre PDF, DOCX ou TXT.");
		}
		String mediaType = normalizeMediaType(file);
		if (mediaType != null && !ALLOWED_MEDIA_TYPES.contains(mediaType)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le type MIME du fichier ne correspond pas aux formats autorises.");
		}
	}

	private Path store(MultipartFile file, String originalFileName) {
		try {
			Files.createDirectories(storageRoot);
			Path target = storageRoot.resolve(UUID.randomUUID() + "-" + originalFileName).normalize();
			if (!target.startsWith(storageRoot)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Le nom du fichier est invalide.");
			}
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return target;
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Le fichier n'a pas pu etre stocke.");
		}
	}

	private String cleanOriginalFileName(String value) {
		String fileName = value == null ? "document.txt" : Path.of(value).getFileName().toString();
		String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.replaceAll("[^A-Za-z0-9._-]", "-")
				.replaceAll("-{2,}", "-")
				.replaceAll("(^-|-$)", "");
		return normalized.isBlank() ? "document.txt" : normalized;
	}

	private String extension(String fileName) {
		String cleanName = cleanOriginalFileName(fileName).toLowerCase(Locale.ROOT);
		int index = cleanName.lastIndexOf('.');
		return index < 0 ? "" : cleanName.substring(index + 1);
	}

	private String normalizeMediaType(MultipartFile file) {
		String mediaType = file.getContentType();
		return mediaType == null || mediaType.isBlank() ? null : mediaType.toLowerCase(Locale.ROOT);
	}

	public record ImportedDocument(String fileName, String filePath, long fileSize, String mediaType, String extractedText) {
	}
}

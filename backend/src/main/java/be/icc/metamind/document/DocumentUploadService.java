package be.icc.metamind.document;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import be.icc.metamind.api.ApiException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentUploadService {
	private static final long MAX_FILE_SIZE = 128L * 1024L * 1024L;
	private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");
	private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
	private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
			"application/pdf",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"text/plain"
	);
	private static final Set<String> ALLOWED_IMAGE_MEDIA_TYPES = Set.of(
			"image/png",
			"image/jpeg",
			"image/webp"
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
		Path storedPath = store(file, storageRoot, originalFileName);
		return new ImportedDocument(originalFileName, storedPath.toString(), file.getSize(), normalizeMediaType(file), extractedText);
	}

	public String storeCoverImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			return null;
		}
		validateCoverImage(image);
		String originalFileName = cleanOriginalFileName(image.getOriginalFilename());
		Path coverRoot = storageRoot.resolve("covers").normalize();
		return store(image, coverRoot, originalFileName).toString();
	}

	public StoredImage loadCoverImage(String storedPath) {
		if (storedPath == null || storedPath.isBlank()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Aucune image de couverture n'est disponible.");
		}
		try {
			Path coverRoot = storageRoot.resolve("covers").normalize();
			Path path = Path.of(storedPath).toAbsolutePath().normalize();
			if (!path.startsWith(coverRoot) || !Files.isRegularFile(path)) {
				throw new ApiException(HttpStatus.NOT_FOUND, "Aucune image de couverture n'est disponible.");
			}
			String mediaType = Files.probeContentType(path);
			return new StoredImage(Files.readAllBytes(path), mediaTypeFromPath(path, mediaType));
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Aucune image de couverture n'est disponible.");
		}
	}

	public StoredFile loadDocumentFile(String storedPath) {
		if (storedPath == null || storedPath.isBlank()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Aucun fichier n'est disponible pour ce document.");
		}
		try {
			Path path = Path.of(storedPath).toAbsolutePath().normalize();
			if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
				throw new ApiException(HttpStatus.NOT_FOUND, "Aucun fichier n'est disponible pour ce document.");
			}
			String mediaType = Files.probeContentType(path);
			return new StoredFile(Files.readAllBytes(path), documentMediaTypeFromPath(path, mediaType));
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Aucun fichier n'est disponible pour ce document.");
		}
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

	private void validateCoverImage(MultipartFile image) {
		if (image.getSize() > MAX_IMAGE_SIZE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "L'image de couverture depasse la taille maximale de 5 MB.");
		}
		String extension = extension(image.getOriginalFilename());
		if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "L'image de couverture doit etre au format PNG, JPEG ou WEBP.");
		}
		String mediaType = normalizeMediaType(image);
		if (mediaType != null && !ALLOWED_IMAGE_MEDIA_TYPES.contains(mediaType)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le type MIME de l'image ne correspond pas aux formats autorises.");
		}
	}

	private Path store(MultipartFile file, Path targetRoot, String originalFileName) {
		try {
			Files.createDirectories(targetRoot);
			Path target = targetRoot.resolve(UUID.randomUUID() + "-" + originalFileName).normalize();
			if (!target.startsWith(targetRoot)) {
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

	private MediaType mediaTypeFromPath(Path path, String probedType) {
		String mediaType = probedType == null || probedType.isBlank() ? null : probedType.toLowerCase(Locale.ROOT);
		if (ALLOWED_IMAGE_MEDIA_TYPES.contains(mediaType)) {
			return MediaType.parseMediaType(mediaType);
		}
		return switch (extension(path.getFileName().toString())) {
			case "png" -> MediaType.IMAGE_PNG;
			case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
			case "webp" -> MediaType.parseMediaType("image/webp");
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}

	private MediaType documentMediaTypeFromPath(Path path, String probedType) {
		String mediaType = probedType == null || probedType.isBlank() ? null : probedType.toLowerCase(Locale.ROOT);
		if (ALLOWED_MEDIA_TYPES.contains(mediaType)) {
			return MediaType.parseMediaType(mediaType);
		}
		return switch (extension(path.getFileName().toString())) {
			case "pdf" -> MediaType.APPLICATION_PDF;
			case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
			case "txt" -> MediaType.TEXT_PLAIN;
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}

	public String storePdfThumbnail(MultipartFile file) {
		if (file == null || file.isEmpty() || !"pdf".equals(extension(file.getOriginalFilename()))) {
			return null;
		}
		try {
			return storePdfThumbnailFromBytes(file.getBytes());
		}
		catch (IOException exception) {
			return null;
		}
	}

	public String storePdfThumbnailFromBytes(byte[] pdfBytes) {
		if (pdfBytes == null || pdfBytes.length == 0) {
			return null;
		}
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			if (document.getNumberOfPages() == 0) {
				return null;
			}
			BufferedImage rendered = new PDFRenderer(document).renderImageWithDPI(0, 110, ImageType.RGB);
			BufferedImage thumbnail = scaleToWidth(rendered, 520);
			Path coverRoot = storageRoot.resolve("covers").normalize();
			Files.createDirectories(coverRoot);
			Path target = coverRoot.resolve(UUID.randomUUID() + "-vignette.jpg").normalize();
			try (OutputStream output = Files.newOutputStream(target)) {
				ImageIO.write(thumbnail, "jpg", output);
			}
			return target.toString();
		}
		catch (IOException exception) {
			return null;
		}
	}

	public byte[] buildTitlePagePdf(String title, String author, int year, String discipline, String institution) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			float margin = 72f;
			float usable = PDRectangle.A4.getWidth() - 2 * margin;
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				float y = 730f;
				content.beginText();
				content.setFont(regular, 11);
				content.newLineAtOffset(margin, y);
				content.showText(sanitizePdfText(discipline == null ? "" : discipline.toUpperCase(Locale.ROOT)));
				content.endText();
				y -= 46f;
				y = drawWrappedTitle(content, bold, 23f, sanitizePdfText(title), margin, y, usable);
				y -= 26f;
				content.beginText();
				content.setFont(regular, 13);
				content.newLineAtOffset(margin, y);
				content.showText(sanitizePdfText(author + "  -  " + year));
				content.endText();
				y -= 22f;
				content.beginText();
				content.setFont(regular, 12);
				content.newLineAtOffset(margin, y);
				content.showText(sanitizePdfText(institution));
				content.endText();
			}
			document.save(output);
			return output.toByteArray();
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "La page de titre n'a pas pu etre generee.");
		}
	}

	public String storeSeedDocumentPdf(byte[] pdfBytes, String fileName) {
		try {
			Files.createDirectories(storageRoot);
			Path target = storageRoot.resolve(UUID.randomUUID() + "-" + cleanOriginalFileName(fileName)).normalize();
			Files.write(target, pdfBytes);
			return target.toString();
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Le document d'exemple n'a pas pu etre stocke.");
		}
	}

	private BufferedImage scaleToWidth(BufferedImage source, int targetWidth) {
		if (source.getWidth() <= targetWidth) {
			return source;
		}
		int targetHeight = Math.round(source.getHeight() * (targetWidth / (float) source.getWidth()));
		BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, targetWidth, targetHeight);
		graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
		graphics.dispose();
		return scaled;
	}

	private float drawWrappedTitle(PDPageContentStream content, PDType1Font font, float size, String text, float x, float y, float maxWidth) throws IOException {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split("\\s+")) {
			String candidate = line.length() == 0 ? word : line + " " + word;
			float width = font.getStringWidth(candidate) / 1000 * size;
			if (width > maxWidth && line.length() > 0) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else {
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0) {
			lines.add(line.toString());
		}
		float leading = size * 1.2f;
		for (String rendered : lines) {
			content.beginText();
			content.setFont(font, size);
			content.newLineAtOffset(x, y);
			content.showText(rendered);
			content.endText();
			y -= leading;
		}
		return y;
	}

	private String sanitizePdfText(String value) {
		if (value == null) {
			return "";
		}
		String cleaned = value
				.replace('\u2019', '\'')
				.replace('\u2018', '\'')
				.replace('\u201C', '"')
				.replace('\u201D', '"')
				.replace('\u2013', '-')
				.replace('\u2014', '-');
		StringBuilder builder = new StringBuilder(cleaned.length());
		for (char character : cleaned.toCharArray()) {
			builder.append(character <= 0xFF ? character : '?');
		}
		return builder.toString();
	}

	public record ImportedDocument(String fileName, String filePath, long fileSize, String mediaType, String extractedText) {
	}

	public record StoredImage(byte[] content, MediaType mediaType) {
	}

	public record StoredFile(byte[] content, MediaType mediaType) {
	}
}

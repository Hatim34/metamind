package be.icc.metamind.publication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.api.PageResponse;
import be.icc.metamind.document.AuthorEntity;
import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorEntity;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentUploadService;
import be.icc.metamind.document.DocumentUploadService.ImportedDocument;
import be.icc.metamind.document.DocumentUploadService.StoredFile;
import be.icc.metamind.document.DocumentUploadService.StoredImage;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.KeywordEntity;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataStatus;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PublicationService {
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final AuthorRepository authorRepository;
	private final KeywordRepository keywordRepository;
	private final DocumentAuthorRepository documentAuthorRepository;
	private final DocumentKeywordRepository documentKeywordRepository;
	private final DocumentUploadService documentUploadService;

	public PublicationService(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			AuthorRepository authorRepository,
			KeywordRepository keywordRepository,
			DocumentAuthorRepository documentAuthorRepository,
			DocumentKeywordRepository documentKeywordRepository,
			DocumentUploadService documentUploadService
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.authorRepository = authorRepository;
		this.keywordRepository = keywordRepository;
		this.documentAuthorRepository = documentAuthorRepository;
		this.documentKeywordRepository = documentKeywordRepository;
		this.documentUploadService = documentUploadService;
	}

	@Transactional(readOnly = true)
	public List<PublicationResponse> findPublications(String search, UserEntity currentUser) {
		String value = Optional.ofNullable(search).orElse("").trim();
		List<DocumentEntity> documents = value.isBlank()
				? documentRepository.findAll()
				: documentRepository.search(value);

		return documents.stream()
				.filter(document -> isVisibleFor(document, currentUser))
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<PublicationResponse> findDocumentsPage(String search, DocumentStatus status, int page, int size, UserEntity currentUser) {
		String value = Optional.ofNullable(search).orElse("").trim();
		List<DocumentEntity> documents = value.isBlank()
				? documentRepository.findAll()
				: documentRepository.search(value);

		List<PublicationResponse> responses = documents.stream()
				.filter(document -> isInManagementScope(document, currentUser))
				.filter(document -> status == null || document.getStatus() == status)
				.map(this::toResponse)
				.toList();
		return PageResponse.from(responses, page, size);
	}

	@Transactional(readOnly = true)
	public PublicationResponse findPublication(long id, UserEntity currentUser) {
		DocumentEntity document = documentRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!isVisibleFor(document, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication n'est pas accessible avec ce compte.");
		}
		return toResponse(document);
	}

	@Transactional(readOnly = true)
	public List<PublicationResponse> findPublicSearch(String search, String author) {
		return buildPublicSearch(search, author);
	}

	@Transactional(readOnly = true)
	public PageResponse<PublicationResponse> findPublicSearchPage(String search, String author, int page, int size) {
		return PageResponse.from(buildPublicSearch(search, author), page, size);
	}

	private List<PublicationResponse> buildPublicSearch(String search, String author) {
		String value = Optional.ofNullable(search).orElse("").trim();
		String authorFilter = Optional.ofNullable(author).orElse("").trim().toLowerCase();
		List<DocumentEntity> documents = value.isBlank()
				? documentRepository.findAll()
				: documentRepository.search(value);

		return documents.stream()
				.filter(document -> document.getStatus() == DocumentStatus.PUBLIE)
				.filter(document -> document.getVisibility() == DocumentVisibility.PUBLIC)
				.map(this::toResponse)
				.filter(publication -> authorFilter.isBlank() || publication.author().toLowerCase().contains(authorFilter))
				.toList();
	}

	@Transactional
	public PublicationResponse createPublication(PublicationRequest request, UserEntity currentUser) {
		return createPublication(request, null, currentUser);
	}

	@Transactional
	public PublicationResponse createPublication(PublicationRequest request, MultipartFile image, UserEntity currentUser) {
		validate(request);

		DocumentEntity document = documentRepository.save(new DocumentEntity(
				fileNameFromTitle(request.title()),
				null,
				0L,
				"TXT",
				request.title().trim(),
				DocumentStatus.A_VALIDER,
				toDocumentVisibility(request.visibility()),
				currentUser.getInstitution(),
				currentUser
		));
		document.updateCoverImagePath(documentUploadService.storeCoverImage(image));
		metadataRepository.save(new MetadataEntity(
				document,
				request.title().trim(),
				null,
				LocalDate.of(request.year(), 1, 1),
				null,
				MetadataStatus.EN_ATTENTE
		));
		AuthorEntity author = findOrCreateAuthor(request.author().trim());
		documentAuthorRepository.save(new DocumentAuthorEntity(document, author, 1));
		findOrCreateKeywords(request.keywords()).forEach(keyword -> documentKeywordRepository.save(new DocumentKeywordEntity(document, keyword)));
		return toResponse(document);
	}

	@Transactional
	public PublicationResponse importDocument(MultipartFile file, Visibility visibility, UserEntity currentUser) {
		return importDocument(file, visibility, null, currentUser);
	}

	@Transactional
	public PublicationResponse importDocument(MultipartFile file, Visibility visibility, MultipartFile image, UserEntity currentUser) {
		ImportedDocument imported = documentUploadService.importFile(file);
		DocumentEntity document = documentRepository.save(new DocumentEntity(
				imported.fileName(),
				imported.filePath(),
				imported.fileSize(),
				imported.mediaType(),
				imported.extractedText(),
				DocumentStatus.EN_ATTENTE,
				toDocumentVisibility(visibility == null ? Visibility.INSTITUTION : visibility),
				currentUser.getInstitution(),
				currentUser
		));
		document.updateCoverImagePath(documentUploadService.storeCoverImage(image));
		metadataRepository.save(new MetadataEntity(
				document,
				titleFromFileName(imported.fileName()),
				null,
				null,
				null,
				MetadataStatus.EN_ATTENTE
		));
		return toResponse(document);
	}

	@Transactional(readOnly = true)
	public StoredImage findCoverImage(long id, UserEntity currentUser) {
		DocumentEntity document = documentRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!isVisibleFor(document, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette image n'est pas accessible avec ce compte.");
		}
		return documentUploadService.loadCoverImage(document.getCoverImagePath());
	}

	@Transactional(readOnly = true)
	public StoredFile findDocumentFile(long id, UserEntity currentUser) {
		DocumentEntity document = documentRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!isVisibleFor(document, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Ce fichier n'est pas accessible avec ce compte.");
		}
		return documentUploadService.loadDocumentFile(document.getFilePath());
	}

	@Transactional
	public PublicationResponse updateStatus(long id, PublicationStatusRequest request, UserEntity currentUser) {
		DocumentEntity document = documentRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!canManage(document, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication ne peut pas etre modifiee avec ce compte.");
		}
		if (request.status() == PublicationStatus.EN_ATTENTE || request.status() == PublicationStatus.EXTRACTION) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ce statut est reserve au traitement interne.");
		}
		document.updateStatus(toDocumentStatus(request.status()));
		return toResponse(document);
	}

	@Transactional
	public PublicationResponse deletePublication(long id, UserEntity currentUser) {
		DocumentEntity document = documentRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!canManage(document, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication ne peut pas etre supprimee avec ce compte.");
		}
		document.updateStatus(DocumentStatus.SUPPRIME);
		return toResponse(document);
	}

	private boolean isVisibleFor(DocumentEntity document, UserEntity currentUser) {
		if (currentUser != null && currentUser.getRole() == UserRole.ADMIN) {
			return true;
		}
		if (currentUser != null && document.getInstitution().getId().equals(currentUser.getInstitution().getId())) {
			return true;
		}
		return document.getStatus() == DocumentStatus.PUBLIE && document.getVisibility() == DocumentVisibility.PUBLIC;
	}

	private boolean canManage(DocumentEntity document, UserEntity currentUser) {
		return currentUser.getRole() == UserRole.ADMIN
				|| document.getInstitution().getId().equals(currentUser.getInstitution().getId());
	}

	private boolean isInManagementScope(DocumentEntity document, UserEntity currentUser) {
		if (currentUser.getRole() == UserRole.ADMIN) {
			return true;
		}
		return document.getInstitution().getId().equals(currentUser.getInstitution().getId());
	}

	private void validate(PublicationRequest request) {
		if (isBlank(request.title()) || isBlank(request.author())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le titre et l'auteur sont obligatoires.");
		}
		if (request.year() < 1900 || request.year() > 2100) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "L'annee de publication est invalide.");
		}
		if (request.visibility() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La visibilite est obligatoire.");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isBlank();
	}

	private String normalizeKeywords(List<String> keywords) {
		if (keywords == null) {
			return "";
		}
		return keywords.stream()
				.map(String::trim)
				.filter(keyword -> !keyword.isBlank())
				.collect(Collectors.joining(","));
	}

	private PublicationResponse toResponse(DocumentEntity document) {
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId()).orElse(null);
		String author = documentAuthorRepository.findByDocument_IdOrderByAuthorOrderAsc(document.getId())
				.stream()
				.map(documentAuthor -> documentAuthor.getAuthor().getFullName())
				.collect(Collectors.joining(", "));
		List<String> keywords = documentKeywordRepository.findByDocument_Id(document.getId())
				.stream()
				.map(documentKeyword -> documentKeyword.getKeyword().getLibelle())
				.toList();
		return PublicationResponse.from(document, metadata, author, keywords);
	}

	private AuthorEntity findOrCreateAuthor(String fullName) {
		return authorRepository.findByFullNameIgnoreCase(fullName)
				.orElseGet(() -> authorRepository.save(new AuthorEntity(fullName, null)));
	}

	private List<KeywordEntity> findOrCreateKeywords(List<String> keywords) {
		return Optional.ofNullable(keywords).orElse(List.of()).stream()
				.map(String::trim)
				.filter(keyword -> !keyword.isBlank())
				.distinct()
				.map(keyword -> keywordRepository.findByLibelleIgnoreCase(keyword)
						.orElseGet(() -> keywordRepository.save(new KeywordEntity(keyword))))
				.toList();
	}

	private String fileNameFromTitle(String title) {
		String normalized = title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return normalized.isBlank() ? "document.txt" : normalized + ".txt";
	}

	private String titleFromFileName(String fileName) {
		String cleanName = fileName == null ? "Document importe" : fileName;
		int index = cleanName.lastIndexOf('.');
		String withoutExtension = index < 0 ? cleanName : cleanName.substring(0, index);
		String normalized = withoutExtension.replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim();
		return normalized.isBlank() ? "Document importe" : normalized;
	}

	private DocumentStatus toDocumentStatus(PublicationStatus status) {
		return DocumentStatus.valueOf(status.name());
	}

	private DocumentVisibility toDocumentVisibility(Visibility visibility) {
		return DocumentVisibility.valueOf(visibility.name());
	}
}

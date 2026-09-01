package be.icc.metamind.document;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataService {
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final AuthorRepository authorRepository;
	private final KeywordRepository keywordRepository;
	private final DocumentAuthorRepository documentAuthorRepository;
	private final DocumentKeywordRepository documentKeywordRepository;

	public MetadataService(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			AuthorRepository authorRepository,
			KeywordRepository keywordRepository,
			DocumentAuthorRepository documentAuthorRepository,
			DocumentKeywordRepository documentKeywordRepository
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.authorRepository = authorRepository;
		this.keywordRepository = keywordRepository;
		this.documentAuthorRepository = documentAuthorRepository;
		this.documentKeywordRepository = documentKeywordRepository;
	}

	@Transactional(readOnly = true)
	public MetadataResponse getMetadata(long documentId, UserEntity user) {
		DocumentEntity document = findManageableDocument(documentId, user);
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Les metadonnees du document sont introuvables."));
		return toResponse(metadata);
	}

	@Transactional
	public MetadataResponse validateMetadata(long documentId, MetadataValidationRequest request, UserEntity user) {
		DocumentEntity document = findManageableDocument(documentId, user);
		if (document.getStatus() == DocumentStatus.SUPPRIME) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Un document supprime ne peut pas etre publie.");
		}
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId())
				.orElseGet(() -> metadataRepository.save(new MetadataEntity(document, document.getFileName(), null, null, null, MetadataStatus.EN_ATTENTE)));

		String title = cleanRequired(request.title(), "Le titre est obligatoire.");
		String summary = cleanOptional(request.summary());
		String classification = cleanOptional(request.classification());
		metadata.validate(title, summary, request.publicationDate(), classification, user);
		replaceAuthors(document, request.authors());
		replaceKeywords(document, request.keywords());
		document.publish(request.visibility(), searchText(title, summary, classification, request.keywords()));
		return toResponse(metadata);
	}

	private DocumentEntity findManageableDocument(long documentId, UserEntity user) {
		DocumentEntity document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le document demande est introuvable."));
		if (user.getRole() != UserRole.ADMIN && !Objects.equals(document.getInstitution().getId(), user.getInstitution().getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Ce document appartient a une autre institution.");
		}
		return document;
	}

	private void replaceAuthors(DocumentEntity document, List<MetadataAuthorRequest> authors) {
		documentAuthorRepository.deleteByDocument_Id(document.getId());
		List<MetadataAuthorRequest> cleanAuthors = Optional.ofNullable(authors).orElse(List.of()).stream()
				.filter(author -> author != null && author.fullName() != null && !author.fullName().trim().isBlank())
				.limit(20)
				.toList();
		if (cleanAuthors.isEmpty()) {
			return;
		}
		int order = 1;
		for (MetadataAuthorRequest request : cleanAuthors) {
			AuthorEntity author = authorRepository.findByFullNameIgnoreCase(request.fullName().trim())
					.orElseGet(() -> authorRepository.save(new AuthorEntity(request.fullName().trim(), cleanOptional(request.orcid()))));
			author.updateOrcid(cleanOptional(request.orcid()));
			documentAuthorRepository.save(new DocumentAuthorEntity(document, author, order++));
		}
	}

	private void replaceKeywords(DocumentEntity document, List<String> keywords) {
		documentKeywordRepository.deleteByDocument_Id(document.getId());
		Optional.ofNullable(keywords).orElse(List.of()).stream()
				.map(this::cleanOptional)
				.filter(keyword -> keyword != null && !keyword.isBlank())
				.distinct()
				.limit(30)
				.map(keyword -> keywordRepository.findByLibelleIgnoreCase(keyword)
						.orElseGet(() -> keywordRepository.save(new KeywordEntity(keyword))))
				.forEach(keyword -> documentKeywordRepository.save(new DocumentKeywordEntity(document, keyword)));
	}

	private MetadataResponse toResponse(MetadataEntity metadata) {
		List<DocumentAuthorEntity> authors = documentAuthorRepository.findByDocument_IdOrderByAuthorOrderAsc(metadata.getDocument().getId());
		List<DocumentKeywordEntity> keywords = documentKeywordRepository.findByDocument_Id(metadata.getDocument().getId());
		return MetadataResponse.from(metadata, authors, keywords);
	}

	private String cleanRequired(String value, String errorMessage) {
		String cleanValue = cleanOptional(value);
		if (cleanValue == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
		}
		return cleanValue;
	}

	private String cleanOptional(String value) {
		if (value == null) {
			return null;
		}
		String cleanValue = value.trim().replaceAll("\\s+", " ");
		return cleanValue.isBlank() ? null : cleanValue;
	}

	private String searchText(String title, String summary, String classification, List<String> keywords) {
		return String.join(" ",
				title == null ? "" : title,
				summary == null ? "" : summary,
				classification == null ? "" : classification,
				String.join(" ", Optional.ofNullable(keywords).orElse(List.of()))
		).trim();
	}
}

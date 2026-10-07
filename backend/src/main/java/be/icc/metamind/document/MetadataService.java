package be.icc.metamind.document;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.api.ClientIpResolver;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;
import be.icc.metamind.opendata.DspacePublisher;

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
	private final AuditLogRepository auditLogRepository;
	private final DspacePublisher dspacePublisher;
	private final ValidationDecisionRecorder validationDecisionRecorder;
	private final LanguageRepository languageRepository;
	private final DocumentTypeRepository documentTypeRepository;

	public MetadataService(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			AuthorRepository authorRepository,
			KeywordRepository keywordRepository,
			DocumentAuthorRepository documentAuthorRepository,
			DocumentKeywordRepository documentKeywordRepository,
			AuditLogRepository auditLogRepository,
			DspacePublisher dspacePublisher,
			ValidationDecisionRecorder validationDecisionRecorder,
			LanguageRepository languageRepository,
			DocumentTypeRepository documentTypeRepository
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.authorRepository = authorRepository;
		this.keywordRepository = keywordRepository;
		this.documentAuthorRepository = documentAuthorRepository;
		this.documentKeywordRepository = documentKeywordRepository;
		this.auditLogRepository = auditLogRepository;
		this.dspacePublisher = dspacePublisher;
		this.validationDecisionRecorder = validationDecisionRecorder;
		this.languageRepository = languageRepository;
		this.documentTypeRepository = documentTypeRepository;
	}

	@Transactional(readOnly = true)
	public MetadataResponse getMetadata(long documentId, UserEntity user) {
		DocumentEntity document = findManageableDocument(documentId, user);
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Les metadonnees du document sont introuvables."));
		return toResponse(metadata);
	}

	@Transactional(readOnly = true)
	public List<MetadataHistoryResponse> getMetadataHistory(long documentId, UserEntity user) {
		DocumentEntity document = findManageableDocument(documentId, user);
		return auditLogRepository.findByActionAndEntityTypeAndEntityIdOrderByCreatedAtDesc("MODIFICATION_METADONNEE", "metadonnees", document.getId())
				.stream()
				.map(MetadataHistoryResponse::from)
				.toList();
	}

	@Transactional
	public MetadataResponse validateMetadata(long documentId, MetadataValidationRequest request, UserEntity user) {
		DocumentEntity document = findManageableDocument(documentId, user);
		if (document.getStatus() == DocumentStatus.SUPPRIME) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Un document supprime ne peut pas etre publie.");
		}
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId())
				.orElseGet(() -> metadataRepository.save(new MetadataEntity(document, document.getFileName(), null, null, null, MetadataStatus.EN_ATTENTE)));

		String previousTitle = metadata.getTitre();
		String previousSummary = metadata.getResume();
		String previousPublicationDate = metadata.getPublicationDate() == null ? null : metadata.getPublicationDate().toString();
		String previousClassification = metadata.getClassification();
		String previousVisibility = document.getVisibility() == null ? null : document.getVisibility().name();
		String previousAuthors = authorsValue(document);
		String previousKeywords = keywordsValue(document);
		String previousLanguage = metadata.getLanguage() == null ? null : metadata.getLanguage().getCode();
		String previousDocumentType = metadata.getDocumentType() == null ? null : metadata.getDocumentType().getCode();
		String previousDoi = metadata.getDoi();

		String title = cleanRequired(request.title(), "Le titre est obligatoire.");
		if (request.authors() == null || request.authors().stream().noneMatch(author -> author != null && cleanOptional(author.fullName()) != null)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Au moins un auteur est obligatoire.");
		}
		if (request.publicationDate() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La date de publication est obligatoire.");
		}
		String summary = cleanOptional(request.summary());
		String classification = cleanOptional(request.classification());
		String doi = cleanOptional(request.doi());
		if (doi != null && metadataRepository.existsByDoiIgnoreCaseAndDocument_IdNot(doi, document.getId())) {
			throw new ApiException(HttpStatus.CONFLICT,
					"Le DOI " + doi + " est deja utilise par un autre document.");
		}
		LanguageEntity language = resolveLanguage(request.language());
		DocumentTypeEntity documentType = resolveDocumentType(request.documentType());
		metadata.validate(title, summary, request.publicationDate(), classification, language, documentType, doi, user);
		replaceAuthors(document, request.authors());
		replaceKeywords(document, request.keywords());
		document.publish(request.visibility(), searchText(title, summary, classification, request.keywords()));
		recordMetadataHistory(document, "titre", previousTitle, title, user);
		recordMetadataHistory(document, "resume", previousSummary, summary, user);
		recordMetadataHistory(document, "date_publication", previousPublicationDate, request.publicationDate() == null ? null : request.publicationDate().toString(), user);
		recordMetadataHistory(document, "classification", previousClassification, classification, user);
		recordMetadataHistory(document, "visibilite", previousVisibility, request.visibility().name(), user);
		recordMetadataHistory(document, "auteurs", previousAuthors, authorsValue(document), user);
		recordMetadataHistory(document, "mots_cles", previousKeywords, keywordsValue(document), user);
		recordMetadataHistory(document, "langue", previousLanguage, request.language(), user);
		recordMetadataHistory(document, "type_document", previousDocumentType, request.documentType(), user);
		recordMetadataHistory(document, "doi", previousDoi, doi, user);
		// Trace l'arbitrage humain champ par champ : base de la mesure de fiabilite du LLM.
		Map<String, String> publishedValues = new LinkedHashMap<>();
		publishedValues.put("titre", nullSafe(title));
		publishedValues.put("resume", nullSafe(summary));
		publishedValues.put("classification", nullSafe(classification));
		publishedValues.put("auteurs", authorsValue(document));
		publishedValues.put("mots_cles", keywordsValue(document));
		publishedValues.put("langue", language == null ? "" : language.getCode());
		publishedValues.put("type_document", documentType == null ? "" : documentType.getCode());
		publishedValues.put("doi", nullSafe(doi));
		publishedValues.put("date_publication",
				request.publicationDate() == null ? "" : request.publicationDate().toString());
		validationDecisionRecorder.recordValidation(document, publishedValues);
		dspacePublisher.publish(document, metadata,
				documentAuthorRepository.findByDocument_IdOrderByAuthorOrderAsc(document.getId()),
				documentKeywordRepository.findByDocument_Id(document.getId()).stream()
						.map(item -> item.getKeyword().getLibelle())
						.toList());
		return toResponse(metadata);
	}

	@Transactional
	public MetadataResponse rejectMetadata(long documentId, MetadataRejectionRequest request, UserEntity user) {
		DocumentEntity document = findManageableDocument(documentId, user);
		if (document.getStatus() == DocumentStatus.SUPPRIME) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Un document supprime ne peut pas etre rejete.");
		}
		String reason = request.reason() == null ? "" : request.reason().trim();
		if (reason.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le motif du rejet est obligatoire.");
		}
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Aucune metadonnee a rejeter."));
		metadata.reject(user);
		// Sans ce statut, un document rejete restait dans la file "a valider" indefiniment.
		document.updateStatus(DocumentStatus.REJETE);
		validationDecisionRecorder.recordRejection(document);
		recordMetadataHistory(document, "rejet", metadata.getStatus().name(), reason, user);
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

	private void recordMetadataHistory(DocumentEntity document, String field, String previousValue, String newValue, UserEntity user) {
		String previous = historyValue(previousValue);
		String current = historyValue(newValue);
		if (Objects.equals(previous, current)) {
			return;
		}
		auditLogRepository.save(new AuditLogEntity(
				user,
				"MODIFICATION_METADONNEE",
				"metadonnees",
				document.getId(),
				field + "\n" + previous + "\n" + current,
				ClientIpResolver.current()
		));
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Resout un code de langue du vocabulaire de reference.
	 * Un code inconnu est refuse plutot que silencieusement ignore : le bibliothecaire doit le savoir.
	 */
	private LanguageEntity resolveLanguage(String code) {
		String cleanCode = cleanOptional(code);
		if (cleanCode == null) {
			return null;
		}
		return languageRepository.findByCodeIgnoreCase(cleanCode)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
						"La langue '" + cleanCode + "' ne fait pas partie des langues reconnues."));
	}

	private DocumentTypeEntity resolveDocumentType(String code) {
		String cleanCode = cleanOptional(code);
		if (cleanCode == null) {
			return null;
		}
		return documentTypeRepository.findByCodeIgnoreCase(cleanCode)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
						"Le type de document '" + cleanCode + "' ne fait pas partie des types reconnus."));
	}

	private String historyValue(String value) {
		if (value == null || value.trim().isBlank()) {
			return "";
		}
		return value.trim().replaceAll("\\s+", " ");
	}

	private String authorsValue(DocumentEntity document) {
		return documentAuthorRepository.findByDocument_IdOrderByAuthorOrderAsc(document.getId())
				.stream()
				.map(documentAuthor -> documentAuthor.getAuthor().getFullName())
				.collect(Collectors.joining(", "));
	}

	private String keywordsValue(DocumentEntity document) {
		return documentKeywordRepository.findByDocument_Id(document.getId())
				.stream()
				.map(documentKeyword -> documentKeyword.getKeyword().getLibelle())
				.collect(Collectors.joining(", "));
	}

	private MetadataResponse toResponse(MetadataEntity metadata) {
		DocumentEntity document = metadata.getDocument();
		List<MetadataAuthorResponse> authors = documentAuthorRepository.findByDocument_IdOrderByAuthorOrderAsc(document.getId())
				.stream()
				.map(link -> new MetadataAuthorResponse(link.getAuthor().getFullName(), link.getAuthor().getOrcid()))
				.toList();
		List<String> keywords = documentKeywordRepository.findByDocument_Id(document.getId())
				.stream()
				.map(link -> link.getKeyword().getLibelle())
				.toList();
		List<MetadataSuggestionEntity> suggestions = validationDecisionRecorder.suggestionsOf(document);
		// Avant validation, auteurs et mots-cles n'existent que comme suggestions du modele :
		// sans elles, le bibliothecaire voyait des champs vides et devait tout ressaisir.
		if (metadata.getStatus() != MetadataStatus.VALIDE) {
			if (authors.isEmpty()) {
				authors = suggestedValues(suggestions, "auteurs").stream()
						.map(name -> new MetadataAuthorResponse(name, null))
						.toList();
			}
			if (keywords.isEmpty()) {
				keywords = suggestedValues(suggestions, "mots_cles");
			}
		}
		return MetadataResponse.from(metadata, authors, keywords, confidences(suggestions));
	}

	/** Valeurs d'un champ multivalue proposees par le modele, dans l'ordre du document. */
	private List<String> suggestedValues(List<MetadataSuggestionEntity> suggestions, String field) {
		return suggestions.stream()
				.filter(suggestion -> field.equals(suggestion.getChamp()))
				.findFirst()
				.map(MetadataSuggestionEntity::getSuggestedValue)
				.map(value -> Arrays.stream(value.split(","))
						.map(String::trim)
						.filter(item -> !item.isEmpty())
						.distinct()
						.toList())
				.orElseGet(List::of);
	}

	private Map<String, Double> confidences(List<MetadataSuggestionEntity> suggestions) {
		Map<String, Double> byField = new LinkedHashMap<>();
		for (MetadataSuggestionEntity suggestion : suggestions) {
			if (suggestion.getConfidenceScore() != null) {
				byField.putIfAbsent(suggestion.getChamp(), suggestion.getConfidenceScore().doubleValue());
			}
		}
		return byField;
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

	private Map<String, String> decisionValues(
			String title,
			String summary,
			String classification,
			String doi,
			LanguageEntity language,
			DocumentTypeEntity documentType,
			MetadataValidationRequest request,
			DocumentEntity document
	) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("titre", nullSafe(title));
		values.put("resume", nullSafe(summary));
		values.put("date_publication", request.publicationDate() == null ? "" : request.publicationDate().toString());
		values.put("classification", nullSafe(classification));
		values.put("langue", language == null ? "" : language.getCode());
		values.put("type_document", documentType == null ? "" : documentType.getCode());
		values.put("doi", nullSafe(doi));
		values.put("auteurs", authorsValue(document));
		values.put("mots_cles", keywordsValue(document));
		return values;
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

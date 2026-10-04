package be.icc.metamind.extraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentTypeRepository;
import be.icc.metamind.document.EnrichmentEntity;
import be.icc.metamind.document.EnrichmentRepository;
import be.icc.metamind.document.EnrichmentStatus;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataSuggestionEntity;
import be.icc.metamind.document.MetadataSuggestionRepository;
import be.icc.metamind.document.MetadataSuggestionSource;
import be.icc.metamind.document.LanguageRepository;
import be.icc.metamind.credit.CreditMovementEntity;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {
	private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(1[89]\\d{2}|20\\d{2}|21\\d{2})\\b");

	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final EnrichmentRepository enrichmentRepository;
	private final MetadataSuggestionRepository suggestionRepository;
	private final CreditMovementRepository movementRepository;
	private final InstitutionRepository institutionRepository;
	private final EntityManager entityManager;
	private final MetadataExtractionProvider extractionProvider;
	private final TextPreparationService textPreparationService;
	private final ConfidenceScorer confidenceScorer;
	private final LanguageRepository languageRepository;
	private final DocumentTypeRepository documentTypeRepository;

	public ExtractionService(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			EnrichmentRepository enrichmentRepository,
		MetadataSuggestionRepository suggestionRepository,
		CreditMovementRepository movementRepository,
		InstitutionRepository institutionRepository,
		EntityManager entityManager,
		MetadataExtractionProvider extractionProvider,
		TextPreparationService textPreparationService,
		ConfidenceScorer confidenceScorer,
		LanguageRepository languageRepository,
		DocumentTypeRepository documentTypeRepository
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.enrichmentRepository = enrichmentRepository;
		this.suggestionRepository = suggestionRepository;
		this.movementRepository = movementRepository;
		this.institutionRepository = institutionRepository;
		this.entityManager = entityManager;
		this.extractionProvider = extractionProvider;
		this.textPreparationService = textPreparationService;
		this.confidenceScorer = confidenceScorer;
		this.languageRepository = languageRepository;
		this.documentTypeRepository = documentTypeRepository;
	}

	@Transactional(noRollbackFor = ApiException.class)
	public MetadataExtractionResponse extract(long publicationId, UserEntity user) {
		DocumentEntity document = documentRepository.findById(publicationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		InstitutionEntity institution = document.getInstitution();

		if (user.getRole() != UserRole.ADMIN && !Objects.equals(document.getInstitution().getId(), user.getInstitution().getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication appartient a une autre institution.");
		}

		if (document.getStatus() == DocumentStatus.SUPPRIME) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Une publication supprimee ne peut pas etre traitee.");
		}
		if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le document ne contient pas de texte extrait.");
		}
		if (!institution.hasCredits()) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Le solde de credits est insuffisant.");
		}
		if (institutionRepository.reserveCredit(institution.getId()) != 1) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Le solde de credits est insuffisant.");
		}
		entityManager.refresh(institution);

		document.updateStatus(DocumentStatus.EXTRACTION);
		EnrichmentEntity enrichment = enrichmentRepository.save(new EnrichmentEntity(
				document,
				user,
				EnrichmentStatus.EN_COURS,
				extractionProvider.modelName(),
				extractionProvider.promptVersion()
		));

		MetadataExtractionData metadata;
		try {
			metadata = extractionProvider.extract(document);
		}
		catch (RuntimeException primaryException) {
			enrichment.markFailed(failureMessage(primaryException));
			document.markExtractionFailed();
			refundCredit(institution);
			throw toApiException(primaryException);
		}

		PreparedDocument preparedDocument = textPreparationService.prepare(document.getExtractedText());
		document.markExtractionCompleted(String.join(",", metadata.keywords()));
		MetadataEntity metadataEntity = metadataRepository.findByDocumentId(document.getId())
				.orElseGet(() -> metadataRepository.save(new MetadataEntity(document, document.getFileName(), null, null, null, be.icc.metamind.document.MetadataStatus.EN_ATTENTE)));
		// Le DOI annonce par le modele ne vaut que s'il est confirme par le texte ; sinon on garde celui detecte.
		// Le DOI de l'en-tete, jamais un DOI cite en bibliographie.
		String doi = availableDoi(
				confirmedDoi(metadata.doi(), preparedDocument, preparedDocument.documentDoi()),
				document.getId());
		LocalDate publicationDate = parsePublicationDate(metadata.publicationDate());

		metadataEntity.markGenerated(metadata.title(), metadata.summary(), metadata.classification());
		metadataEntity.updateExtractedReferences(
				referenceByCode(preparedDocument.language(), languageRepository::findByCodeIgnoreCase),
				referenceByCode(preparedDocument.documentType(), documentTypeRepository::findByCodeIgnoreCase),
				doi,
				publicationDate
		);
		enrichment.markCompleted(rawResponse(metadata));
		saveSuggestions(enrichment, metadata, preparedDocument);
		saveSuggestion(enrichment, "langue", preparedDocument.language(), preparedDocument);
		saveSuggestion(enrichment, "type_document", preparedDocument.documentType(), preparedDocument);
		saveSuggestion(enrichment, "date_publication",
				publicationDate == null ? null : publicationDate.toString(), preparedDocument, metadata);
		if (doi != null) {
			saveSuggestion(enrichment, "doi", doi, preparedDocument, metadata);
		}
		entityManager.refresh(institution);
		movementRepository.save(new CreditMovementEntity(
				institution,
				CreditMovementType.CONSOMMATION,
				-1,
				enrichment
		));

		return new MetadataExtractionResponse(
				enrichment.getId(),
				enrichment.getStatus().name(),
				document.getId(),
				document.getFileName(),
				metadata.title(),
				metadata.author(),
				metadata.keywords(),
				institution.getCreditBalance()
		);
	}

	/**
	 * Retient le DOI annonce par le modele seulement s'il apparait reellement dans le document.
	 * Un DOI invente est ecarte au profit de celui detecte par expression reguliere, s'il existe.
	 */
	private String confirmedDoi(String modelDoi, PreparedDocument preparedDocument, String detectedDoi) {
		if (modelDoi == null || modelDoi.isBlank()) {
			return detectedDoi;
		}
		String candidate = modelDoi.trim();
		boolean presentInText = preparedDocument.dois().stream().anyMatch(candidate::equalsIgnoreCase)
				|| preparedDocument.normalizedText().toLowerCase(Locale.ROOT)
						.contains(candidate.toLowerCase(Locale.ROOT));
		return presentInText ? candidate : detectedDoi;
	}

	/**
	 * Un DOI identifie une seule publication. S'il est deja porte par un autre document,
	 * la valeur n'est pas reprise : l'extraction est une proposition, elle ne doit pas
	 * faire echouer le traitement sur une violation de contrainte.
	 */
	private String availableDoi(String doi, Long documentId) {
		if (doi == null || doi.isBlank()) {
			return null;
		}
		return metadataRepository.existsByDoiIgnoreCaseAndDocument_IdNot(doi, documentId) ? null : doi;
	}

	/** Lit la date proposee par le modele ; une date illisible est ignoree plutot que corrigee. */
	private LocalDate parsePublicationDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim());
		}
		catch (DateTimeParseException ignored) {
			return yearOnly(value.trim());
		}
	}

	private LocalDate yearOnly(String value) {
		Matcher matcher = YEAR_PATTERN.matcher(value);
		if (!matcher.find()) {
			return null;
		}
		int year = Integer.parseInt(matcher.group());
		if (year < 1900 || year > Year.now().getValue() + 1) {
			return null;
		}
		return LocalDate.of(year, 1, 1);
	}

	/** Resout une valeur de reference sans interroger la base quand le code n'a pas ete detecte. */
	private <T> T referenceByCode(String code, java.util.function.Function<String, java.util.Optional<T>> finder) {
		if (code == null || code.isBlank()) {
			return null;
		}
		return finder.apply(code).orElse(null);
	}

	private void refundCredit(InstitutionEntity institution) {
		institutionRepository.refundCredit(institution.getId());
		entityManager.refresh(institution);
		movementRepository.save(new CreditMovementEntity(
				institution,
				CreditMovementType.REMBOURSEMENT,
				1,
				institution.getCreditBalance(),
				"Remboursement apres echec de l'extraction"
		));
	}

	private String failureMessage(RuntimeException exception) {
		return exception instanceof ApiException ? exception.getMessage() : "Extraction interrompue.";
	}

	private ApiException toApiException(RuntimeException exception) {
		if (exception instanceof ApiException apiException) {
			return apiException;
		}
		return new ApiException(HttpStatus.BAD_GATEWAY, "L'extraction des metadonnees a echoue.");
	}

	@Transactional(noRollbackFor = ApiException.class)
	public MetadataExtractionBatchResponse extractBatch(MetadataExtractionBatchRequest request, UserEntity user) {
		List<Long> documentIds = cleanDocumentIds(request);
		List<MetadataExtractionBatchItemResponse> results = new ArrayList<>();
		for (Long documentId : documentIds) {
			try {
				results.add(MetadataExtractionBatchItemResponse.success(extract(documentId, user)));
			}
			catch (ApiException exception) {
				results.add(MetadataExtractionBatchItemResponse.failure(documentId, exception.getMessage()));
			}
		}
		return MetadataExtractionBatchResponse.from(results);
	}

	private List<Long> cleanDocumentIds(MetadataExtractionBatchRequest request) {
		if (request == null || request.documentIds() == null || request.documentIds().isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La liste des documents est obligatoire.");
		}
		List<Long> documentIds = request.documentIds().stream()
				.filter(Objects::nonNull)
				.filter(id -> id > 0)
				.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
		if (documentIds.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La liste des documents est invalide.");
		}
		if (documentIds.size() > 10) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le traitement groupe est limite a 10 documents.");
		}
		return documentIds;
	}

	private void saveSuggestions(EnrichmentEntity enrichment, MetadataExtractionData metadata, PreparedDocument preparedDocument) {
		saveSuggestion(enrichment, "titre", metadata.title(), preparedDocument, metadata);
		saveSuggestion(enrichment, "auteurs", metadata.author(), preparedDocument, metadata);
		saveSuggestion(enrichment, "resume", metadata.summary(), preparedDocument, metadata);
		saveSuggestion(enrichment, "classification", metadata.classification(), preparedDocument, metadata);
		saveSuggestion(enrichment, "mots_cles", String.join(", ", metadata.keywords()), preparedDocument, metadata);
	}

	/** Champs deduits localement (langue, type, DOI) : le modele n'annonce aucune confiance pour eux. */
	private void saveSuggestion(EnrichmentEntity enrichment, String field, String value, PreparedDocument preparedDocument) {
		saveSuggestion(enrichment, field, value, preparedDocument, null);
	}

	private void saveSuggestion(
			EnrichmentEntity enrichment,
			String field,
			String value,
			PreparedDocument preparedDocument,
			MetadataExtractionData metadata
	) {
		String evidence = evidenceFor(value, preparedDocument.normalizedText());
		Double modelConfidence = metadata == null ? null : metadata.modelConfidenceOf(field);
		ConfidenceScore score = confidenceScorer.score(field, value, evidence, modelConfidence, preparedDocument);
		suggestionRepository.save(new MetadataSuggestionEntity(
				enrichment,
				field,
				value,
				BigDecimal.valueOf(score.value()).setScale(2, RoundingMode.HALF_UP),
				evidence,
				String.join(" | ", score.signals()),
				segmentsFor(evidence, preparedDocument),
				MetadataSuggestionSource.LLM,
				null
		));
	}

	private String evidenceFor(String value, String text) {
		if (value == null || value.isBlank()) {
			return null;
		}
		int start = text.toLowerCase(java.util.Locale.ROOT).indexOf(value.toLowerCase(java.util.Locale.ROOT));
		if (start < 0) {
			return null;
		}
		return text.substring(start, Math.min(text.length(), start + 200));
	}

	private String segmentsFor(String evidence, PreparedDocument preparedDocument) {
		if (evidence == null || evidence.isBlank()) {
			return "";
		}
		return preparedDocument.segments().stream()
				.filter(segment -> segment.text().toLowerCase(java.util.Locale.ROOT)
						.contains(evidence.toLowerCase(java.util.Locale.ROOT)))
				.map(PreparedDocument.Segment::id)
				.collect(Collectors.joining(","));
	}

	private String rawResponse(MetadataExtractionData metadata) {
		return "{"
				+ "\"title\":\"" + escape(metadata.title()) + "\","
				+ "\"author\":\"" + escape(metadata.author()) + "\","
				+ "\"summary\":\"" + escape(metadata.summary()) + "\","
				+ "\"classification\":\"" + escape(metadata.classification()) + "\","
				+ "\"keywords\":\"" + escape(metadata.keywords().stream().collect(Collectors.joining(", "))) + "\""
				+ "}";
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}

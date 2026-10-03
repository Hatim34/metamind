package be.icc.metamind.extraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.EnrichmentEntity;
import be.icc.metamind.document.EnrichmentRepository;
import be.icc.metamind.document.EnrichmentStatus;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataSuggestionEntity;
import be.icc.metamind.document.MetadataSuggestionRepository;
import be.icc.metamind.document.MetadataSuggestionSource;
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
		ConfidenceScorer confidenceScorer
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
				"v1"
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

		document.markExtractionCompleted(String.join(",", metadata.keywords()));
		MetadataEntity metadataEntity = metadataRepository.findByDocumentId(document.getId())
				.orElseGet(() -> metadataRepository.save(new MetadataEntity(document, document.getFileName(), null, null, null, be.icc.metamind.document.MetadataStatus.EN_ATTENTE)));
		metadataEntity.markGenerated(metadata.title(), metadata.summary(), metadata.classification());
		enrichment.markCompleted(rawResponse(metadata));
		saveSuggestions(enrichment, metadata, textPreparationService.prepare(document.getExtractedText()));
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
		saveSuggestion(enrichment, "titre", metadata.title(), preparedDocument);
		saveSuggestion(enrichment, "auteurs", metadata.author(), preparedDocument);
		saveSuggestion(enrichment, "resume", metadata.summary(), preparedDocument);
		saveSuggestion(enrichment, "classification", metadata.classification(), preparedDocument);
		saveSuggestion(enrichment, "mots_cles", String.join(", ", metadata.keywords()), preparedDocument);
	}

	private void saveSuggestion(EnrichmentEntity enrichment, String field, String value, PreparedDocument preparedDocument) {
		String evidence = evidenceFor(value, preparedDocument.normalizedText());
		ConfidenceScore score = confidenceScorer.score(field, value, evidence, 0.5, preparedDocument);
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

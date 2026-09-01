package be.icc.metamind.extraction;

import java.math.BigDecimal;
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
import be.icc.metamind.credit.CreditMovementEntity;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final EnrichmentRepository enrichmentRepository;
	private final MetadataSuggestionRepository suggestionRepository;
	private final CreditMovementRepository movementRepository;
	private final MetadataExtractionProvider extractionProvider;

	public ExtractionService(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			EnrichmentRepository enrichmentRepository,
			MetadataSuggestionRepository suggestionRepository,
			CreditMovementRepository movementRepository,
			MetadataExtractionProvider extractionProvider
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.enrichmentRepository = enrichmentRepository;
		this.suggestionRepository = suggestionRepository;
		this.movementRepository = movementRepository;
		this.extractionProvider = extractionProvider;
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
		catch (ApiException exception) {
			enrichment.markFailed(exception.getMessage());
			document.markExtractionFailed();
			throw exception;
		}
		catch (RuntimeException exception) {
			enrichment.markFailed("Extraction interrompue.");
			document.markExtractionFailed();
			throw new ApiException(HttpStatus.BAD_GATEWAY, "L'extraction des metadonnees a echoue.");
		}

		document.markExtractionCompleted(String.join(",", metadata.keywords()));
		MetadataEntity metadataEntity = metadataRepository.findByDocumentId(document.getId())
				.orElseGet(() -> metadataRepository.save(new MetadataEntity(document, document.getFileName(), null, null, null, be.icc.metamind.document.MetadataStatus.EN_ATTENTE)));
		metadataEntity.markGenerated(metadata.title(), metadata.summary(), metadata.classification());
		enrichment.markCompleted(rawResponse(metadata));
		saveSuggestions(enrichment, metadata);
		institution.consumeCredit();
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

	private void saveSuggestions(EnrichmentEntity enrichment, MetadataExtractionData metadata) {
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "titre", metadata.title(), BigDecimal.valueOf(0.90)));
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "auteurs", metadata.author(), BigDecimal.valueOf(0.80)));
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "resume", metadata.summary(), BigDecimal.valueOf(0.78)));
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "classification", metadata.classification(), BigDecimal.valueOf(0.75)));
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "mots_cles", String.join(", ", metadata.keywords()), BigDecimal.valueOf(0.85)));
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

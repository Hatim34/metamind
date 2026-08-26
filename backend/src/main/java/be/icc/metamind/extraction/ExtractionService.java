package be.icc.metamind.extraction;

import java.util.Objects;

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

	@Transactional
	public MetadataExtractionResponse extract(long publicationId, UserEntity user) {
		DocumentEntity document = documentRepository.findById(publicationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		InstitutionEntity institution = user.getInstitution();

		if (!Objects.equals(document.getInstitution().getId(), institution.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication appartient a une autre institution.");
		}

		if (!institution.hasCredits()) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Le solde de credits est insuffisant.");
		}

		document.updateStatus(DocumentStatus.EXTRACTION);
		EnrichmentEntity enrichment = enrichmentRepository.save(new EnrichmentEntity(
				document,
				user,
				EnrichmentStatus.EN_COURS,
				"local",
				"v1"
		));
		institution.consumeCredit();
		MetadataExtractionData metadata = extractionProvider.extract(document);
		document.markExtractionCompleted(String.join(",", metadata.keywords()));
		MetadataEntity metadataEntity = metadataRepository.findByDocumentId(document.getId())
				.orElseGet(() -> metadataRepository.save(new MetadataEntity(document, document.getFileName(), null, null, null, be.icc.metamind.document.MetadataStatus.EN_ATTENTE)));
		metadataEntity.markGenerated(metadata.title(), null);
		enrichment.markCompleted("{\"title\":\"" + metadata.title().replace("\"", "") + "\"}");
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "titre", metadata.title(), java.math.BigDecimal.valueOf(0.90)));
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "auteurs", metadata.author(), java.math.BigDecimal.valueOf(0.80)));
		suggestionRepository.save(new MetadataSuggestionEntity(enrichment, "mots_cles", String.join(", ", metadata.keywords()), java.math.BigDecimal.valueOf(0.85)));
		movementRepository.save(new CreditMovementEntity(
				institution,
				CreditMovementType.CONSOMMATION,
				-1,
				enrichment
		));

		return new MetadataExtractionResponse(
				document.getId(),
				document.getFileName(),
				metadata.title(),
				metadata.author(),
				metadata.keywords(),
				institution.getCreditBalance()
		);
	}
}

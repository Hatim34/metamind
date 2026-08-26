package be.icc.metamind.extraction;

import java.util.Objects;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.credit.CreditMovementEntity;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.user.UserEntity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {
	private final PublicationRepository publicationRepository;
	private final CreditMovementRepository movementRepository;
	private final MetadataExtractionProvider extractionProvider;

	public ExtractionService(PublicationRepository publicationRepository, CreditMovementRepository movementRepository, MetadataExtractionProvider extractionProvider) {
		this.publicationRepository = publicationRepository;
		this.movementRepository = movementRepository;
		this.extractionProvider = extractionProvider;
	}

	@Transactional
	public MetadataExtractionResponse extract(long publicationId, UserEntity user) {
		PublicationEntity publication = publicationRepository.findById(publicationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		InstitutionEntity institution = user.getInstitution();

		if (!Objects.equals(publication.getInstitution().getId(), institution.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication appartient a une autre institution.");
		}

		if (!institution.hasCredits()) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Le solde de credits est insuffisant.");
		}

		institution.consumeCredit();
		MetadataExtractionData metadata = extractionProvider.extract(publication);
		publication.markExtractionCompleted(String.join(",", metadata.keywords()));
		movementRepository.save(new CreditMovementEntity(
				institution,
				CreditMovementType.CONSOMMATION,
				-1,
				institution.getCreditBalance(),
				"Extraction de metadonnees"
		));

		return new MetadataExtractionResponse(
				publication.getId(),
				publication.getTitle(),
				metadata.title(),
				metadata.author(),
				metadata.keywords(),
				institution.getCreditBalance()
		);
	}
}

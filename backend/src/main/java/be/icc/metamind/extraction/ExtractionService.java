package be.icc.metamind.extraction;

import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {
	private final PublicationRepository publicationRepository;
	private final UserRepository userRepository;

	public ExtractionService(PublicationRepository publicationRepository, UserRepository userRepository) {
		this.publicationRepository = publicationRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public MetadataExtractionResponse extract(long publicationId, long userId) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
		PublicationEntity publication = publicationRepository.findById(publicationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		InstitutionEntity institution = user.getInstitution();

		if (!institution.hasCredits()) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Le solde de credits est insuffisant.");
		}

		institution.consumeCredit();
		List<String> keywords = suggestedKeywords(publication);
		publication.markExtractionCompleted(String.join(",", keywords));

		return new MetadataExtractionResponse(
				publication.getId(),
				publication.getTitle(),
				publication.getTitle(),
				publication.getAuthor(),
				keywords,
				institution.getCreditBalance()
		);
	}

	private List<String> suggestedKeywords(PublicationEntity publication) {
		String title = publication.getTitle().toLowerCase();
		if (title.contains("metadonnees")) {
			return List.of("metadonnees", "Dublin Core", "catalogage");
		}
		if (title.contains("multilingue")) {
			return List.of("multilingue", "indexation", "recherche");
		}
		return List.of("publication", "validation", "bibliotheque");
	}
}

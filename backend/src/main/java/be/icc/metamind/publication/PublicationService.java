package be.icc.metamind.publication;

import java.util.List;
import java.util.Optional;

import be.icc.metamind.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicationService {
	private final PublicationRepository publicationRepository;

	public PublicationService(PublicationRepository publicationRepository) {
		this.publicationRepository = publicationRepository;
	}

	@Transactional(readOnly = true)
	public List<PublicationResponse> findPublications(String search) {
		String value = Optional.ofNullable(search).orElse("").trim();
		List<PublicationEntity> publications = value.isBlank()
				? publicationRepository.findAll()
				: publicationRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrKeywordsTextContainingIgnoreCase(value, value, value);

		return publications.stream()
				.map(PublicationResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PublicationResponse findPublication(long id) {
		return publicationRepository.findById(id)
				.map(PublicationResponse::from)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
	}
}

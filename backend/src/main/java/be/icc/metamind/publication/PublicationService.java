package be.icc.metamind.publication;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicationService {
	private final PublicationRepository publicationRepository;
	private final InstitutionRepository institutionRepository;

	public PublicationService(PublicationRepository publicationRepository, InstitutionRepository institutionRepository) {
		this.publicationRepository = publicationRepository;
		this.institutionRepository = institutionRepository;
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

	@Transactional
	public PublicationResponse createPublication(PublicationRequest request) {
		validate(request);
		InstitutionEntity institution = institutionRepository.findByNameIgnoreCase(request.institution().trim())
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "L'institution indiquee est inconnue."));

		PublicationEntity publication = publicationRepository.save(new PublicationEntity(
				request.title().trim(),
				request.author().trim(),
				request.year(),
				PublicationStatus.A_VALIDER,
				request.visibility(),
				normalizeKeywords(request.keywords()),
				institution
		));
		return PublicationResponse.from(publication);
	}

	private void validate(PublicationRequest request) {
		if (isBlank(request.title()) || isBlank(request.author()) || isBlank(request.institution())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le titre, l'auteur et l'institution sont obligatoires.");
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
}

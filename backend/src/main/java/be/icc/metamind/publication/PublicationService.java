package be.icc.metamind.publication;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

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
	public List<PublicationResponse> findPublications(String search, UserEntity currentUser) {
		String value = Optional.ofNullable(search).orElse("").trim();
		List<PublicationEntity> publications = value.isBlank()
				? publicationRepository.findAll()
				: publicationRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrKeywordsTextContainingIgnoreCase(value, value, value);

		return publications.stream()
				.filter(publication -> isVisibleFor(publication, currentUser))
				.map(PublicationResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PublicationResponse findPublication(long id, UserEntity currentUser) {
		PublicationEntity publication = publicationRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!isVisibleFor(publication, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication n'est pas accessible avec ce compte.");
		}
		return PublicationResponse.from(publication);
	}

	@Transactional
	public PublicationResponse createPublication(PublicationRequest request, UserEntity currentUser) {
		validate(request);

		PublicationEntity publication = publicationRepository.save(new PublicationEntity(
				request.title().trim(),
				request.author().trim(),
				request.year(),
				PublicationStatus.A_VALIDER,
				request.visibility(),
				normalizeKeywords(request.keywords()),
				currentUser.getInstitution()
		));
		return PublicationResponse.from(publication);
	}

	@Transactional
	public PublicationResponse updateStatus(long id, PublicationStatusRequest request, UserEntity currentUser) {
		PublicationEntity publication = publicationRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		if (!canManage(publication, currentUser)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette publication ne peut pas etre modifiee avec ce compte.");
		}
		if (request.status() == PublicationStatus.EN_ATTENTE || request.status() == PublicationStatus.EXTRACTION) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ce statut est reserve au traitement interne.");
		}
		publication.updateStatus(request.status());
		return PublicationResponse.from(publication);
	}

	private boolean isVisibleFor(PublicationEntity publication, UserEntity currentUser) {
		if (currentUser != null && currentUser.getRole() == UserRole.ADMINISTRATEUR) {
			return true;
		}
		if (currentUser != null && publication.getInstitution().getId().equals(currentUser.getInstitution().getId())) {
			return true;
		}
		return publication.getStatus() == PublicationStatus.PUBLIE && publication.getVisibility() == Visibility.PUBLIC;
	}

	private boolean canManage(PublicationEntity publication, UserEntity currentUser) {
		return currentUser.getRole() == UserRole.ADMINISTRATEUR
				|| publication.getInstitution().getId().equals(currentUser.getInstitution().getId());
	}

	private void validate(PublicationRequest request) {
		if (isBlank(request.title()) || isBlank(request.author())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le titre et l'auteur sont obligatoires.");
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

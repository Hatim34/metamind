package be.icc.metamind.document;

import java.util.Optional;

import be.icc.metamind.document.DocumentUploadService.StoredImage;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conserve les couvertures en base, seul stockage durable en production.
 *
 * Le disque du conteneur est recree a chaque deploiement sur Render : une couverture
 * qui n'existe que sur ce disque disparait. La vignette generee a l'import est donc
 * recopiee ici, et c'est cette copie qui est servie au catalogue.
 */
@Service
public class DocumentCoverService {
	private static final int MAX_STORED_BYTES = 3 * 1024 * 1024;

	private final DocumentCoverRepository coverRepository;
	private final DocumentUploadService documentUploadService;

	public DocumentCoverService(
			DocumentCoverRepository coverRepository,
			DocumentUploadService documentUploadService
	) {
		this.coverRepository = coverRepository;
		this.documentUploadService = documentUploadService;
	}

	/**
	 * Recopie en base la couverture presente a ce chemin.
	 * Sans fichier lisible, rien n'est enregistre : l'import ne doit pas echouer
	 * parce qu'une vignette n'a pas pu etre produite.
	 */
	@Transactional
    public void persistFromPath(Long documentId, String coverPath) {
		if (documentId == null) {
			return;
		}
		documentUploadService.readCoverIfPresent(coverPath)
				.filter(image -> image.content().length > 0 && image.content().length <= MAX_STORED_BYTES)
				.ifPresent(image -> store(documentId, image));
	}

	private void store(Long documentId, StoredImage image) {
		String mediaType = image.mediaType() == null
				? MediaType.IMAGE_JPEG_VALUE
				: image.mediaType().toString();
		coverRepository.findById(documentId)
				.map(existing -> {
					existing.replace(image.content(), mediaType);
					return existing;
				})
				.or(() -> Optional.of(new DocumentCoverEntity(documentId, image.content(), mediaType)))
				.ifPresent(coverRepository::save);
	}

	@Transactional(readOnly = true)
	public Optional<StoredImage> load(Long documentId) {
		if (documentId == null) {
			return Optional.empty();
		}
		return coverRepository.findById(documentId)
				.filter(cover -> cover.getData() != null && cover.getData().length > 0)
				.map(cover -> new StoredImage(cover.getData(), MediaType.parseMediaType(cover.getMediaType())));
	}
}

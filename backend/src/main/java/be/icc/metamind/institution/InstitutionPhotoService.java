package be.icc.metamind.institution;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Photos des institutions : choisies par l'administrateur, affichees sur l'accueil avec leur credit. */
@Service
public class InstitutionPhotoService {
	private static final long MAX_BYTES = 5L * 1024L * 1024L;
	private static final Set<String> IMAGE_TYPES = Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp");

	private final InstitutionPhotoRepository photoRepository;
	private final InstitutionRepository institutionRepository;

	public InstitutionPhotoService(InstitutionPhotoRepository photoRepository, InstitutionRepository institutionRepository) {
		this.photoRepository = photoRepository;
		this.institutionRepository = institutionRepository;
	}

	@Transactional
	public InstitutionPhotoResponse replace(long institutionId, MultipartFile image, String credit) {
		InstitutionEntity institution = institutionRepository.findById(institutionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution est introuvable."));
		String type = image == null ? null : image.getContentType();
		if (image == null || image.isEmpty() || type == null || !IMAGE_TYPES.contains(type)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La photo doit etre une image JPEG, PNG ou WebP.");
		}
		if (image.getSize() > MAX_BYTES) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La photo depasse la taille maximale de 5 Mo.");
		}
		String cleanCredit = credit == null ? "" : credit.trim();
		// Une photo sous licence libre doit citer son auteur et sa licence.
		if (cleanCredit.isEmpty() || cleanCredit.length() > 300) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le credit de la photo est obligatoire (300 caracteres au plus).");
		}
		byte[] content;
		try {
			content = image.getBytes();
		}
		catch (IOException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La photo n'a pas pu etre lue.");
		}
		InstitutionPhotoEntity photo = photoRepository.findById(institutionId)
				.map(existing -> {
					existing.replace(content, type, cleanCredit);
					return existing;
				})
				.orElseGet(() -> new InstitutionPhotoEntity(institutionId, content, type, cleanCredit));
		photoRepository.save(photo);
		return toResponse(institution, photo);
	}

	/** Photos des institutions actives, pour l'accueil et l'administration. */
	@Transactional(readOnly = true)
	public List<InstitutionPhotoResponse> list() {
		Map<Long, InstitutionEntity> institutions = institutionRepository.findAll().stream()
				.filter(InstitutionEntity::isActive)
				.collect(Collectors.toMap(InstitutionEntity::getId, Function.identity()));
		return photoRepository.findAll().stream()
				.filter(photo -> institutions.containsKey(photo.getInstitutionId()))
				.map(photo -> toResponse(institutions.get(photo.getInstitutionId()), photo))
				.toList();
	}

	@Transactional(readOnly = true)
	public InstitutionPhotoEntity load(long institutionId) {
		return photoRepository.findById(institutionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cette institution n'a pas de photo."));
	}

	private InstitutionPhotoResponse toResponse(InstitutionEntity institution, InstitutionPhotoEntity photo) {
		// La version change a chaque remplacement : le navigateur ne garde pas l'ancienne photo.
		long version = photo.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
		return new InstitutionPhotoResponse(institution.getId(), institution.getName(),
				"/api/v1/institutions/" + institution.getId() + "/photo?v=" + Long.toHexString(version), photo.getCredit());
	}
}

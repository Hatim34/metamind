package be.icc.metamind.document;

import be.icc.metamind.api.PageResponse;
import be.icc.metamind.document.DocumentUploadService.StoredFile;
import be.icc.metamind.document.DocumentUploadService.StoredImage;
import be.icc.metamind.publication.PublicationResponse;
import be.icc.metamind.publication.PublicationService;
import be.icc.metamind.publication.Visibility;
import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
	private final PublicationService publicationService;
	private final AccountService accountService;

	public DocumentController(PublicationService publicationService, AccountService accountService) {
		this.publicationService = publicationService;
		this.accountService = accountService;
	}

	@GetMapping
	public PageResponse<PublicationResponse> list(
			@RequestHeader("Authorization") String authorization,
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "statut", required = false) DocumentStatus status,
			@RequestParam(value = "date_debut", required = false) LocalDate startDate,
			@RequestParam(value = "date_fin", required = false) LocalDate endDate,
			@RequestParam(value = "institution_id", required = false) Long institutionId,
			@RequestParam(value = "institutionId", required = false) Long legacyInstitutionId,
			@RequestParam(value = "sort", required = false) String sort,
			@RequestParam(value = "tri", required = false) String legacySort,
			@RequestParam(value = "direction", required = false) String direction,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		UserEntity currentUser = accountService.authenticate(authorization);
		Long selectedInstitutionId = institutionId == null ? legacyInstitutionId : institutionId;
		String selectedSort = sort == null ? legacySort : sort;
		return publicationService.findDocumentsPage(query, status, startDate, endDate, selectedInstitutionId, selectedSort, direction, page, size, currentUser);
	}

	@GetMapping("/{id}")
	public PublicationResponse detail(
			@PathVariable long id,
			@RequestHeader(value = "Authorization", required = false) String authorization
	) {
		UserEntity currentUser = authorization == null ? null : accountService.authenticate(authorization);
		return publicationService.findPublication(id, currentUser);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PublicationResponse importDocument(
			@RequestHeader("Authorization") String authorization,
			@RequestParam(value = "fichier", required = false) MultipartFile fichier,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "image", required = false) MultipartFile image,
			@RequestParam(value = "visibilite", required = false) Visibility visibility
	) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return publicationService.importDocument(fichier == null ? file : fichier, visibility, image, currentUser);
	}

	@GetMapping("/{id}/image")
	public ResponseEntity<byte[]> image(
			@PathVariable long id,
			@RequestHeader(value = "Authorization", required = false) String authorization
	) {
		UserEntity currentUser = authorization == null ? null : accountService.authenticate(authorization);
		StoredImage image = publicationService.findCoverImage(id, currentUser);
		return ResponseEntity.ok()
				.contentType(image.mediaType())
				.body(image.content());
	}

	@GetMapping("/{id}/file")
	public ResponseEntity<byte[]> file(
			@PathVariable long id,
			@RequestHeader(value = "Authorization", required = false) String authorization
	) {
		UserEntity currentUser = authorization == null ? null : accountService.authenticate(authorization);
		StoredFile file = publicationService.findDocumentFile(id, currentUser);
		return ResponseEntity.ok()
				.contentType(file.mediaType())
				.body(file.content());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		publicationService.deletePublication(id, currentUser);
		return ResponseEntity.noContent().build();
	}
}

package be.icc.metamind.document;

import be.icc.metamind.api.PageResponse;
import be.icc.metamind.publication.PublicationResponse;
import be.icc.metamind.publication.PublicationService;
import be.icc.metamind.publication.Visibility;
import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

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
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return publicationService.findDocumentsPage(query, status, page, size, currentUser);
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
			@RequestParam(value = "visibilite", required = false) Visibility visibility
	) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return publicationService.importDocument(fichier == null ? file : fichier, visibility, currentUser);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		publicationService.deletePublication(id, currentUser);
		return ResponseEntity.noContent().build();
	}
}

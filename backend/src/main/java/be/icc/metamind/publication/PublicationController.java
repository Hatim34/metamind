package be.icc.metamind.publication;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

@RestController
@RequestMapping("/api/v1/publications")
public class PublicationController {
	private final PublicationService service;
	private final AccountService accountService;

	public PublicationController(PublicationService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@GetMapping
	public List<PublicationResponse> list(@RequestParam(required = false) String search, @RequestHeader(value = "Authorization", required = false) String authorization) {
		UserEntity currentUser = authorization == null ? null : accountService.authenticate(authorization);
		return service.findPublications(search, currentUser);
	}

	@GetMapping("/{id}")
	public PublicationResponse detail(@PathVariable long id, @RequestHeader(value = "Authorization", required = false) String authorization) {
		UserEntity currentUser = authorization == null ? null : accountService.authenticate(authorization);
		return service.findPublication(id, currentUser);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PublicationResponse create(@RequestHeader("Authorization") String authorization, @Valid @RequestBody PublicationRequest request) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return service.createPublication(request, currentUser);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PublicationResponse importDocument(
			@RequestHeader("Authorization") String authorization,
			@RequestParam(value = "fichier", required = false) MultipartFile fichier,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "image", required = false) MultipartFile image,
			@RequestParam(value = "visibilite", required = false) Visibility visibility,
			@RequestParam(value = "visibility", required = false) Visibility visibilityAlias,
			@RequestParam(value = "titre", required = false) String titre,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "auteur", required = false) String auteur,
			@RequestParam(value = "author", required = false) String author,
			@RequestParam(value = "annee", required = false) Integer annee,
			@RequestParam(value = "year", required = false) Integer year,
			@RequestParam(value = "mots_cles", required = false) List<String> motsCles,
			@RequestParam(value = "keywords", required = false) List<String> keywords
	) {
		UserEntity currentUser = accountService.authenticate(authorization);
		MultipartFile uploadedFile = fichier == null ? file : fichier;
		Visibility selectedVisibility = visibility == null ? visibilityAlias : visibility;
		if (uploadedFile != null && !uploadedFile.isEmpty()) {
			return service.importDocument(uploadedFile, selectedVisibility, image, currentUser);
		}
		PublicationRequest request = new PublicationRequest(
				firstText(titre, title),
				firstText(auteur, author),
				null,
				firstYear(annee, year),
				selectedVisibility,
				firstKeywords(motsCles, keywords)
		);
		return service.createPublication(request, image, currentUser);
	}

	private String firstText(String first, String second) {
		return first == null ? second : first;
	}

	private int firstYear(Integer first, Integer second) {
		return first == null ? second == null ? 0 : second : first;
	}

	private List<String> firstKeywords(List<String> first, List<String> second) {
		return first == null || first.isEmpty() ? second : first;
	}

	@PutMapping("/{id}/status")
	public PublicationResponse updateStatus(@PathVariable long id, @RequestHeader("Authorization") String authorization, @Valid @RequestBody PublicationStatusRequest request) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return service.updateStatus(id, request, currentUser);
	}

	@DeleteMapping("/{id}")
	public PublicationResponse delete(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return service.deletePublication(id, currentUser);
	}
}

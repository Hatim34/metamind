package be.icc.metamind.institution;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;

import be.icc.metamind.user.AccountService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {
	private final InstitutionService service;
	private final AccountService accountService;
	private final InstitutionPhotoService photoService;

	public InstitutionController(InstitutionService service, AccountService accountService, InstitutionPhotoService photoService) {
		this.service = service;
		this.accountService = accountService;
		this.photoService = photoService;
	}

	/** Photos et credits des institutions actives (public : affiches sur l'accueil). */
	@GetMapping("/photos")
	public List<InstitutionPhotoResponse> photos() {
		return photoService.list();
	}

	@GetMapping("/{id}/photo")
	public ResponseEntity<byte[]> photo(@PathVariable long id) {
		InstitutionPhotoEntity photo = photoService.load(id);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
				.contentType(MediaType.parseMediaType(photo.getMediaType()))
				.body(photo.getData());
	}

	/** Remplace la photo d'une institution et son credit (administrateur). */
	@PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public InstitutionPhotoResponse replacePhoto(
			@PathVariable long id,
			@RequestHeader("Authorization") String authorization,
			@RequestParam("image") MultipartFile image,
			@RequestParam("credit") String credit
	) {
		accountService.authenticateAdmin(authorization);
		return photoService.replace(id, image, credit);
	}

	@GetMapping
	public List<InstitutionResponse> list(@RequestHeader("Authorization") String authorization) {
		accountService.authenticateAdmin(authorization);
		return service.findAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InstitutionResponse create(@RequestHeader("Authorization") String authorization, @Valid @RequestBody InstitutionRequest request) {
		accountService.authenticateAdmin(authorization);
		return service.create(request);
	}

	@DeleteMapping("/{id}")
	public InstitutionResponse deactivate(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		accountService.authenticateAdmin(authorization);
		return service.deactivate(id);
	}
}

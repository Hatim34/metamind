package be.icc.metamind.user;

import be.icc.metamind.api.ApiException;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {
	private final AccountService service;

	public ProfileController(AccountService service) {
		this.service = service;
	}

	@GetMapping("/{id}/profile")
	public UserResponse getProfile(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		service.authenticateSelfOrAdmin(id, authorization);
		return service.getProfile(id);
	}

	@GetMapping("/{id}/data-export")
	public PersonalDataExportResponse exportPersonalData(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		service.authenticateSelfOrAdmin(id, authorization);
		return service.exportPersonalData(id);
	}

	@PutMapping("/{id}/profile")
	public UserResponse updateProfile(@PathVariable long id, @RequestHeader("Authorization") String authorization, @Valid @RequestBody UpdateProfileRequest request) {
		service.authenticateSelfOrAdmin(id, authorization);
		return service.updateProfile(id, request);
	}

	/** Seule la personne elle-meme change son mot de passe, en confirmant l'actuel. */
	@PutMapping("/{id}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@PathVariable long id, @RequestHeader("Authorization") String authorization, @Valid @RequestBody ChangePasswordRequest request) {
		UserEntity currentUser = service.authenticate(authorization);
		if (currentUser.getId() != id) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Cette action n'est pas autorisee pour ce compte.");
		}
		service.changePassword(id, request);
	}

	@DeleteMapping("/{id}")
	public UserResponse requestDeletion(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		service.authenticateSelfOrAdmin(id, authorization);
		return service.requestAccountDeletion(id);
	}
}

package be.icc.metamind.user;

import be.icc.metamind.core.MetamindService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {
	private final MetamindService service;

	public ProfileController(MetamindService service) {
		this.service = service;
	}

	@GetMapping("/{id}/profile")
	public UserResponse getProfile(@PathVariable long id) {
		return service.getProfile(id);
	}

	@PutMapping("/{id}/profile")
	public UserResponse updateProfile(@PathVariable long id, @Valid @RequestBody UpdateProfileRequest request) {
		return service.updateProfile(id, request);
	}

	@DeleteMapping("/{id}")
	public UserResponse requestDeletion(@PathVariable long id) {
		return service.requestAccountDeletion(id);
	}
}

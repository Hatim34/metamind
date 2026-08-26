package be.icc.metamind.publication;

import java.util.List;

import jakarta.validation.Valid;

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
import org.springframework.http.HttpStatus;

import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

@RestController
@RequestMapping({"/api/v1/publications", "/api/v1/documents"})
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

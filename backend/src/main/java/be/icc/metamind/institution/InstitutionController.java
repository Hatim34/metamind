package be.icc.metamind.institution;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import be.icc.metamind.user.AccountService;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {
	private final InstitutionService service;
	private final AccountService accountService;

	public InstitutionController(InstitutionService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
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

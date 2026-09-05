package be.icc.metamind.admin;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import be.icc.metamind.api.PageResponse;
import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
	private final AdminService service;
	private final AccountService accountService;

	public AdminController(AdminService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@GetMapping("/users")
	public PageResponse<UserResponse> users(
			@RequestHeader("Authorization") String authorization,
			@RequestParam(required = false) Long institutionId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		accountService.authenticateAdmin(authorization);
		return service.listUsers(institutionId, page, size);
	}

	@PatchMapping("/users/{id}")
	public UserResponse updateUser(@PathVariable long id, @RequestHeader("Authorization") String authorization, @Valid @RequestBody AdminUserUpdateRequest request) {
		UserEntity admin = accountService.authenticateAdmin(authorization);
		return service.updateUser(id, request, admin);
	}

	@GetMapping("/institutions")
	public List<InstitutionResponse> institutions(@RequestHeader("Authorization") String authorization) {
		accountService.authenticateAdmin(authorization);
		return service.listInstitutions();
	}

	@GetMapping("/config")
	public Map<String, String> configuration(@RequestHeader("Authorization") String authorization) {
		accountService.authenticateAdmin(authorization);
		return service.readConfiguration();
	}

	@GetMapping("/logs")
	public PageResponse<AuditLogResponse> logs(
			@RequestHeader("Authorization") String authorization,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		accountService.authenticateAdmin(authorization);
		return service.listLogs(page, size);
	}
}

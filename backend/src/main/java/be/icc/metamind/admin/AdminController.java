package be.icc.metamind.admin;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import be.icc.metamind.api.PageResponse;
import be.icc.metamind.document.DocumentFileService;
import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.publication.PublicationResponse;
import be.icc.metamind.publication.PublicationService;
import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
	private final AdminService service;
	private final AccountService accountService;
	private final DocumentFileService documentFileService;
	private final PublicationService publicationService;

	public AdminController(AdminService service, AccountService accountService, DocumentFileService documentFileService, PublicationService publicationService) {
		this.service = service;
		this.accountService = accountService;
		this.documentFileService = documentFileService;
		this.publicationService = publicationService;
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

	@PatchMapping("/institutions/{id}")
	public InstitutionResponse updateInstitution(
			@PathVariable long id,
			@RequestHeader("Authorization") String authorization,
			@RequestBody AdminInstitutionUpdateRequest request
	) {
		UserEntity admin = accountService.authenticateAdmin(authorization);
		return service.updateInstitution(id, request, admin);
	}

	@GetMapping("/config")
	public Map<String, String> configuration(@RequestHeader("Authorization") String authorization) {
		accountService.authenticateAdmin(authorization);
		return service.readConfiguration();
	}

	@PatchMapping("/config")
	public Map<String, String> updateConfiguration(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, String> values) {
		UserEntity admin = accountService.authenticateAdmin(authorization);
		return service.updateConfiguration(values, admin);
	}

	@PutMapping("/config")
	public Map<String, String> replaceConfiguration(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, String> values) {
		UserEntity admin = accountService.authenticateAdmin(authorization);
		return service.updateConfiguration(values, admin);
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

	/**
	 * Rattache un fichier aux documents importes sous ce nom dont le fichier a ete perdu
	 * (documents importes avant que les fichiers soient conserves en base).
	 */
	@PostMapping(value = "/documents/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileRestoreResponse restoreDocumentFile(
			@RequestHeader("Authorization") String authorization,
			@RequestParam("fichier") MultipartFile file
	) {
		accountService.authenticateAdmin(authorization);
		return new FileRestoreResponse(file.getOriginalFilename(), documentFileService.restoreMissingFiles(file));
	}

	/** Remplace l'image d'un document (vignette du catalogue et de la fiche). */
	@PutMapping(value = "/documents/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public PublicationResponse replaceDocumentImage(
			@PathVariable long id,
			@RequestHeader("Authorization") String authorization,
			@RequestParam("image") MultipartFile image
	) {
		accountService.authenticateAdmin(authorization);
		return publicationService.replaceCoverImage(id, image);
	}

	@GetMapping(value = "/reports/documents.csv", produces = "text/csv")
	public ResponseEntity<String> exportDocuments(@RequestHeader("Authorization") String authorization) {
		accountService.authenticateAdmin(authorization);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metamind-documents.csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(service.exportDocumentsCsv());
	}
}

package be.icc.metamind.document;

import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/metadata")
public class MetadataController {
	private final MetadataService metadataService;
	private final AccountService accountService;

	public MetadataController(MetadataService metadataService, AccountService accountService) {
		this.metadataService = metadataService;
		this.accountService = accountService;
	}

	@GetMapping
	public MetadataResponse getMetadata(@PathVariable long documentId, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return metadataService.getMetadata(documentId, currentUser);
	}

	@GetMapping("/historique")
	public List<MetadataHistoryResponse> getMetadataHistory(@PathVariable long documentId, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return metadataService.getMetadataHistory(documentId, currentUser);
	}

	@PutMapping
	public MetadataResponse validateMetadata(@PathVariable long documentId, @RequestHeader("Authorization") String authorization, @Valid @RequestBody MetadataValidationRequest request) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return metadataService.validateMetadata(documentId, request, currentUser);
	}
}

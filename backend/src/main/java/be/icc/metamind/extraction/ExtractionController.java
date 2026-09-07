package be.icc.metamind.extraction;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

@RestController
@RequestMapping("/api/v1")
public class ExtractionController {
	private final ExtractionService service;
	private final AccountService accountService;

	public ExtractionController(ExtractionService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@PostMapping({"/publications/{publicationId}/extraction", "/documents/{publicationId}/extraction"})
	@ResponseStatus(HttpStatus.ACCEPTED)
	public MetadataExtractionResponse extract(@PathVariable long publicationId, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return service.extract(publicationId, currentUser);
	}

	@PostMapping("/documents/extractions")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public MetadataExtractionBatchResponse extractBatch(@RequestBody MetadataExtractionBatchRequest request, @RequestHeader("Authorization") String authorization) {
		UserEntity currentUser = accountService.authenticate(authorization);
		return service.extractBatch(request, currentUser);
	}
}

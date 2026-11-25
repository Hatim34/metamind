package be.icc.metamind.invoice;

import java.nio.charset.StandardCharsets;
import java.util.List;

import be.icc.metamind.institution.InstitutionResponse;
import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InvoiceController {
	private final InvoiceService service;
	private final AccountService accountService;

	public InvoiceController(InvoiceService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@GetMapping("/factures")
	public List<InvoiceResponse> list(@RequestHeader("Authorization") String authorization) {
		return service.list(accountService.authenticate(authorization));
	}

	@GetMapping("/factures/{id}/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		UserEntity user = accountService.authenticate(authorization);
		return file(service.pdf(id, user), MediaType.APPLICATION_PDF, "Facture-" + service.number(id, user) + ".pdf");
	}

	@GetMapping("/factures/{id}/xml")
	public ResponseEntity<byte[]> xml(@PathVariable long id, @RequestHeader("Authorization") String authorization) {
		UserEntity user = accountService.authenticate(authorization);
		return file(service.xml(id, user).getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML,
				"Facture-" + service.number(id, user) + ".xml");
	}

	@PutMapping("/admin/institutions/{id}/facturation")
	public InstitutionResponse updateBilling(@PathVariable long id, @RequestHeader("Authorization") String authorization,
			@Valid @RequestBody InstitutionBillingRequest request) {
		return service.updateBilling(id, request, accountService.authenticateAdmin(authorization));
	}

	private ResponseEntity<byte[]> file(byte[] content, MediaType type, String name) {
		return ResponseEntity.ok()
				.contentType(type)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
				.body(content);
	}
}

package be.icc.metamind.publication;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/publications")
public class PublicationController {
	private final PublicationService service;

	public PublicationController(PublicationService service) {
		this.service = service;
	}

	@GetMapping
	public List<PublicationResponse> list(@RequestParam(required = false) String search) {
		return service.findPublications(search);
	}

	@GetMapping("/{id}")
	public PublicationResponse detail(@PathVariable long id) {
		return service.findPublication(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PublicationResponse create(@RequestBody PublicationRequest request) {
		return service.createPublication(request);
	}
}

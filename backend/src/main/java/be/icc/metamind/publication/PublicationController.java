package be.icc.metamind.publication;

import java.util.List;

import be.icc.metamind.core.MetamindService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publications")
public class PublicationController {
	private final MetamindService service;

	public PublicationController(MetamindService service) {
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
}

package be.icc.metamind.publication;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
	private final PublicationService service;

	public SearchController(PublicationService service) {
		this.service = service;
	}

	@GetMapping
	public List<PublicationResponse> search(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(required = false) String author
	) {
		return service.findPublicSearch(query, author);
	}
}

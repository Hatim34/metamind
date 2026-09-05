package be.icc.metamind.publication;

import be.icc.metamind.api.PageResponse;

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
	public PageResponse<PublicationResponse> search(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(required = false) String author,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return service.findPublicSearchPage(query, author, page, size);
	}
}

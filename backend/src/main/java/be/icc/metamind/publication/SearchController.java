package be.icc.metamind.publication;

import java.time.LocalDate;

import be.icc.metamind.api.PageResponse;

import org.springframework.format.annotation.DateTimeFormat;
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
			@RequestParam(value = "langue", required = false) String language,
			@RequestParam(value = "type", required = false) String documentType,
			@RequestParam(value = "date_debut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "date_fin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return service.findPublicSearchPage(query, author, language, documentType, startDate, endDate, page, size);
	}
}

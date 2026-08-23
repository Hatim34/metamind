package be.icc.metamind.extraction;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publications/{publicationId}/extraction")
public class ExtractionController {
	private final ExtractionService service;

	public ExtractionController(ExtractionService service) {
		this.service = service;
	}

	@PostMapping
	public MetadataExtractionResponse extract(@PathVariable long publicationId, @RequestParam long userId) {
		return service.extract(publicationId, userId);
	}
}

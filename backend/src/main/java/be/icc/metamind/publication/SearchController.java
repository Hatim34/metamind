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
	private final PublicationTranslationService translationService;

	public SearchController(PublicationService service, PublicationTranslationService translationService) {
		this.service = service;
		this.translationService = translationService;
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
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(value = "affichage", required = false) String displayLanguage
	) {
		PageResponse<PublicationResponse> result = service.findPublicSearchPage(query, author, language, documentType, startDate, endDate, page, size);
		// Langue choisie par le visiteur : titres, resumes et mots-cles traduits s'ils sont prets.
		return new PageResponse<>(translationService.localized(result.contenu(), displayLanguage),
				result.page(), result.size(), result.totalElements(), result.totalPages());
	}
}

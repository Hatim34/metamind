package be.icc.metamind.document;

import java.util.Comparator;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose les vocabulaires controles (langues, types de documents).
 * Donnees de reference non sensibles : accessibles sans jeton, comme le catalogue public,
 * afin de servir aussi bien l'ecran de validation que les filtres du catalogue.
 */
@RestController
@RequestMapping("/api/v1/references")
public class ReferenceDataController {
	private final LanguageRepository languageRepository;
	private final DocumentTypeRepository documentTypeRepository;

	public ReferenceDataController(
			LanguageRepository languageRepository,
			DocumentTypeRepository documentTypeRepository
	) {
		this.languageRepository = languageRepository;
		this.documentTypeRepository = documentTypeRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public ReferenceDataResponse getReferences() {
		List<ReferenceDataResponse.ReferenceValue> languages = languageRepository.findAll().stream()
				.map(language -> new ReferenceDataResponse.ReferenceValue(language.getCode(), language.getLibelle()))
				.sorted(Comparator.comparing(ReferenceDataResponse.ReferenceValue::label))
				.toList();
		List<ReferenceDataResponse.ReferenceValue> documentTypes = documentTypeRepository.findAll().stream()
				.map(type -> new ReferenceDataResponse.ReferenceValue(type.getCode(), type.getLibelle()))
				.sorted(Comparator.comparing(ReferenceDataResponse.ReferenceValue::label))
				.toList();
		return new ReferenceDataResponse(languages, documentTypes);
	}
}

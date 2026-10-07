package be.icc.metamind.publication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.user.UserEntity;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Gere la traduction a la demande et son cache en base. */
@Service
public class PublicationTranslationService {
	private static final List<String> SUPPORTED_LANGUAGES = List.of("fr", "nl", "en");

	private final PublicationService publicationService;
	private final DocumentRepository documentRepository;
	private final PublicationTranslationRepository translationRepository;
	private final ObjectProvider<PublicationTranslationProvider> translationProvider;
	private final ObjectMapper objectMapper;

	public PublicationTranslationService(
			PublicationService publicationService,
			DocumentRepository documentRepository,
			PublicationTranslationRepository translationRepository,
			ObjectProvider<PublicationTranslationProvider> translationProvider,
			ObjectMapper objectMapper
	) {
		this.publicationService = publicationService;
		this.documentRepository = documentRepository;
		this.translationRepository = translationRepository;
		this.translationProvider = translationProvider;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public PublicationTranslation translate(long documentId, String requestedLanguage, UserEntity currentUser) {
		String targetLanguage = normalizeLanguage(requestedLanguage);
		PublicationResponse publication = publicationService.findPublication(documentId, currentUser);
		String sourceLanguage = normalizeSourceLanguage(publication.language());
		if (sourceLanguage.equals(targetLanguage)) {
			return PublicationTranslation.source(publication, targetLanguage);
		}

		TranslationSource source = new TranslationSource(publication.title(), publication.summary(), publication.keywords());
		String fingerprint = fingerprint(source);
		PublicationTranslationEntity cached = translationRepository
				.findByDocumentIdAndTargetLanguage(documentId, targetLanguage)
				.orElse(null);
		if (cached != null && fingerprint.equals(cached.getSourceFingerprint())) {
			return new PublicationTranslation(targetLanguage, sourceLanguage, cached.getTitle(), cached.getSummary(), readKeywords(cached.getKeywordsJson()), true);
		}

		PublicationTranslationProvider provider = provider();
		PublicationTranslation translated = provider.translate(source, sourceLanguage, targetLanguage);
		DocumentEntity document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		String keywordsJson = writeKeywords(translated.keywords());
		if (cached == null) {
			cached = new PublicationTranslationEntity(document, targetLanguage, fingerprint, translated.title(), translated.summary(), keywordsJson, provider.modelName());
		} else {
			cached.refresh(fingerprint, translated.title(), translated.summary(), keywordsJson, provider.modelName());
		}
		translationRepository.save(cached);
		return new PublicationTranslation(targetLanguage, sourceLanguage, translated.title(), translated.summary(), translated.keywords(), translated.translated());
	}

	private PublicationTranslationProvider provider() {
		return translationProvider.getIfAvailable(() -> new LocalPublicationTranslationProvider());
	}

	private String normalizeLanguage(String language) {
		String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
		if (!SUPPORTED_LANGUAGES.contains(normalized)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La langue de traduction doit etre fr, nl ou en.");
		}
		return normalized;
	}

	private String normalizeSourceLanguage(String language) {
		String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
		return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : "en";
	}

	private String fingerprint(TranslationSource source) {
		try {
			String value = String.join("\u001f", source.title() == null ? "" : source.title(), source.summary() == null ? "" : source.summary(), String.join("\u001e", source.keywords()));
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (Exception exception) {
			throw new IllegalStateException("Impossible de calculer l'empreinte de la notice.", exception);
		}
	}

	private String writeKeywords(List<String> keywords) {
		try {
			return objectMapper.writeValueAsString(keywords == null ? List.of() : keywords);
		} catch (Exception exception) {
			throw new IllegalStateException("Impossible d'enregistrer les mots-cles traduits.", exception);
		}
	}

	private List<String> readKeywords(String keywordsJson) {
		if (keywordsJson == null || keywordsJson.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(keywordsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
		} catch (Exception exception) {
			return List.of();
		}
	}
}

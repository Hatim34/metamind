package be.icc.metamind.publication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
	/** A changer quand les consignes de traduction changent : toutes les notices sont alors retraduites. */
	static final String TRANSLATION_PROMPT_VERSION = "traduction-v2";

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

	/**
	 * Traduction deja disponible : la notice est dans la langue demandee, ou sa traduction
	 * en cache correspond encore a la notice. Aucun appel au modele.
	 */
	public Optional<PublicationTranslation> ready(long documentId, String requestedLanguage, UserEntity currentUser) {
		String targetLanguage = normalizeLanguage(requestedLanguage);
		PublicationResponse publication = publicationService.findPublication(documentId, currentUser);
		return ready(publication, targetLanguage);
	}

	/** Notice d'origine, affichee le temps que la traduction soit preparee. */
	public PublicationTranslation original(long documentId, String requestedLanguage, UserEntity currentUser) {
		return PublicationTranslation.source(publicationService.findPublication(documentId, currentUser), normalizeLanguage(requestedLanguage));
	}

	/**
	 * Traduit puis met en cache. Volontairement hors transaction : l'appel au modele peut
	 * durer, il ne doit pas garder une connexion a la base pendant ce temps.
	 */
	public PublicationTranslation translate(long documentId, String requestedLanguage, UserEntity currentUser) {
		String targetLanguage = normalizeLanguage(requestedLanguage);
		PublicationResponse publication = publicationService.findPublication(documentId, currentUser);
		Optional<PublicationTranslation> available = ready(publication, targetLanguage);
		if (available.isPresent()) {
			return available.get();
		}

		String sourceLanguage = sourceLanguage(publication);
		TranslationSource source = new TranslationSource(publication.title(), publication.summary(), publication.keywords(), publication.classification());
		String fingerprint = fingerprint(source);
		PublicationTranslationProvider provider = provider();
		PublicationTranslation translated = provider.translate(source, sourceLanguage, targetLanguage);
		DocumentEntity document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication demandee est introuvable."));
		String keywordsJson = writeKeywords(translated.keywords());
		PublicationTranslationEntity cached = translationRepository
				.findByDocumentIdAndTargetLanguage(documentId, targetLanguage)
				.orElse(null);
		if (cached == null) {
			cached = new PublicationTranslationEntity(document, targetLanguage, fingerprint, translated.title(), translated.summary(), keywordsJson, provider.modelName());
		} else {
			cached.refresh(fingerprint, translated.title(), translated.summary(), keywordsJson, provider.modelName());
		}
		cached.translateClassification(translated.classification());
		translationRepository.save(cached);
		return new PublicationTranslation(targetLanguage, sourceLanguage, translated.title(), translated.summary(), translated.keywords(), translated.translated(),
				translated.classification() == null ? publication.classification() : translated.classification());
	}

	private Optional<PublicationTranslation> ready(PublicationResponse publication, String targetLanguage) {
		String sourceLanguage = sourceLanguage(publication);
		if (sourceLanguage.equals(targetLanguage)) {
			return Optional.of(PublicationTranslation.source(publication, targetLanguage));
		}
		String fingerprint = fingerprint(new TranslationSource(publication.title(), publication.summary(), publication.keywords(), publication.classification()));
		return translationRepository.findByDocumentIdAndTargetLanguage(publication.id(), targetLanguage)
				.filter(cached -> fingerprint.equals(cached.getSourceFingerprint()))
				.map(cached -> new PublicationTranslation(targetLanguage, sourceLanguage, cached.getTitle(), cached.getSummary(), readKeywords(cached.getKeywordsJson()), true,
						cached.getClassification() == null ? publication.classification() : cached.getClassification()));
	}

	/**
	 * Applique aux listes (catalogue, accueil) les traductions deja preparees.
	 * Aucun appel au modele ici : une liste doit s'afficher immediatement. Une notice
	 * pas encore traduite reste dans sa langue d'origine.
	 */
	@Transactional(readOnly = true)
	public List<PublicationResponse> localized(List<PublicationResponse> publications, String requestedLanguage) {
		String target = requestedLanguage == null ? "" : requestedLanguage.trim().toLowerCase(Locale.ROOT);
		if (!SUPPORTED_LANGUAGES.contains(target) || publications.isEmpty()) {
			return publications;
		}
		List<Long> ids = publications.stream()
				.filter(publication -> !target.equals(sourceLanguage(publication)))
				.map(PublicationResponse::id)
				.toList();
		if (ids.isEmpty()) {
			return publications;
		}
		Map<Long, PublicationTranslationEntity> byDocument = translationRepository.findByTargetLanguageAndDocument_IdIn(target, ids).stream()
				.collect(Collectors.toMap(translation -> translation.getDocument().getId(), translation -> translation, (first, second) -> first));
		return publications.stream()
				.map(publication -> {
					PublicationTranslationEntity translation = byDocument.get(publication.id());
					return translation == null ? publication : publication.withDisplayText(
							translation.getTitle(), translation.getSummary(), readKeywords(translation.getKeywordsJson()), translation.getClassification());
				})
				.toList();
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

	/**
	 * Langue du titre et du resume tels qu'ils seront affiches. La langue enregistree de la
	 * notice ne sert que si le texte ne permet pas de trancher : une notice marquee « anglais »
	 * dont le titre est en francais doit quand meme etre traduite vers l'anglais.
	 */
	private String sourceLanguage(PublicationResponse publication) {
		String guessed = TextLanguage.guess(publication.title() + " " + (publication.summary() == null ? "" : publication.summary()));
		return guessed != null ? guessed : normalizeSourceLanguage(publication.language());
	}

	private String normalizeSourceLanguage(String language) {
		String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
		return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : "en";
	}

	private String fingerprint(TranslationSource source) {
		try {
			// La version des consignes fait partie de l'empreinte : quand elles changent, les traductions sont refaites.
			String value = String.join("\u001f", TRANSLATION_PROMPT_VERSION, source.title() == null ? "" : source.title(), source.summary() == null ? "" : source.summary(), String.join("\u001e", source.keywords()),
					source.classification() == null ? "" : source.classification());
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

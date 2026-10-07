package be.icc.metamind.publication;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.config.PlatformSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Appel Gemini borne a la traduction des metadonnees visibles. */
@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "gemini")
public class GeminiPublicationTranslationProvider implements PublicationTranslationProvider {
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;
	private final PlatformSettings settings;

	public GeminiPublicationTranslationProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, String apiKey, String model) {
		this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
		this.settings = null;
	}

	@Autowired
	public GeminiPublicationTranslationProvider(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			@Value("${metamind.gemini.api-key:}") String apiKey,
			@Value("${metamind.gemini.model:gemini-3.5-flash-lite}") String model,
			PlatformSettings settings
	) {
		// Sans limite, un modele sature faisait attendre plusieurs minutes ; l'essai suivant est plus utile.
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(10));
		requestFactory.setReadTimeout(Duration.ofSeconds(60));
		this.restClient = restClientBuilder.clone().requestFactory(requestFactory).baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
		this.settings = settings;
	}

	@Override
	public PublicationTranslation translate(TranslationSource source, String sourceLanguage, String targetLanguage) {
		if (apiKey.isBlank()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La traduction est indisponible : la cle Gemini n'est pas configuree.");
		}
		String prompt = """
				Tu traduis les metadonnees d'une publication universitaire.
				Traduis du %s vers le %s. Ne traduis ni les noms propres, ni les DOI, ni les noms d'auteurs.
				N'invente aucune information. Garde les mots-cles sous forme de liste courte.
				Reponds uniquement avec un objet JSON : {"title": string, "summary": string|null, "keywords": string[], "classification": string|null}.
				<metadata>
				{"title":%s,"summary":%s,"keywords":%s,"classification":%s}
				</metadata>
				""".formatted(
				languageName(sourceLanguage),
				languageName(targetLanguage),
				toJson(source.title()),
				toJson(source.summary()),
				toJson(source.keywords()),
				toJson(source.classification())
		);
		GeminiRequest request = new GeminiRequest(List.of(new GeminiContent(List.of(new GeminiPart(prompt)))), new GenerationConfig("application/json"));
		JsonNode response = restClient.post()
				.uri("/v1beta/models/{model}:generateContent", modelName())
				.header("x-goog-api-key", apiKey)
				.body(request)
				.retrieve()
				.body(JsonNode.class);
		return parse(response, sourceLanguage, targetLanguage);
	}

	@Override
	public String modelName() {
		return settings == null ? model : settings.llmModel();
	}

	private PublicationTranslation parse(JsonNode response, String sourceLanguage, String targetLanguage) {
		String text = StreamSupport.stream(response.path("candidates").path(0).path("content").path("parts").spliterator(), false)
				.map(part -> part.path("text").asText(""))
				.reduce("", String::concat)
				.replace("```json", "").replace("```", "").trim();
		if (text.isBlank()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Gemini n'a pas renvoye de traduction.");
		}
		try {
			JsonNode json = objectMapper.readTree(text);
			String title = textOrNull(json.path("title"));
			if (title == null) {
				throw new ApiException(HttpStatus.BAD_GATEWAY, "Gemini n'a pas renvoye de titre traduit.");
			}
			return new PublicationTranslation(targetLanguage, sourceLanguage, title, textOrNull(json.path("summary")), keywords(json.path("keywords")), true,
					textOrNull(json.path("classification")));
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La reponse de traduction de Gemini est inexploitable.");
		}
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception exception) {
			throw new IllegalStateException("Les metadonnees ne peuvent pas etre preparees pour la traduction.", exception);
		}
	}

	private String textOrNull(JsonNode node) {
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		String value = node.asText("").trim();
		return value.isBlank() ? null : value;
	}

	private List<String> keywords(JsonNode node) {
		if (!node.isArray()) {
			return List.of();
		}
		return StreamSupport.stream(node.spliterator(), false)
				.map(JsonNode::asText)
				.map(String::trim)
				.filter(keyword -> !keyword.isBlank())
				.limit(12)
				.toList();
	}

	private String languageName(String code) {
		return switch (code.toLowerCase(Locale.ROOT)) {
			case "fr" -> "francais";
			case "nl" -> "neerlandais";
			case "en" -> "anglais";
			default -> code;
		};
	}

	private record GeminiRequest(List<GeminiContent> contents, GenerationConfig generationConfig) {
	}

	private record GeminiContent(List<GeminiPart> parts) {
	}

	private record GeminiPart(String text) {
	}

	private record GenerationConfig(String responseMimeType) {
	}
}

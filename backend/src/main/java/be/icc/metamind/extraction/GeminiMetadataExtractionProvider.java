package be.icc.metamind.extraction;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.config.PlatformSettings;
import be.icc.metamind.document.DocumentEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "gemini")
public class GeminiMetadataExtractionProvider implements MetadataExtractionProvider {
	private static final String PROMPT_VERSION = "extraction-v5";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;
	private final TextPreparationService textPreparationService;
	private final String promptTemplate;
	private final PlatformSettings settings;

	public GeminiMetadataExtractionProvider(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			String apiKey,
			String model,
			TextPreparationService textPreparationService
	) {
		this(restClientBuilder, objectMapper, apiKey, model, textPreparationService, null);
	}

	@Autowired
	public GeminiMetadataExtractionProvider(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			@Value("${metamind.gemini.api-key:}") String apiKey,
			@Value("${metamind.gemini.model:gemini-3.5-flash-lite}") String model,
			TextPreparationService textPreparationService,
			PlatformSettings settings
	) {
		this.settings = settings;
		this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
		this.textPreparationService = textPreparationService;
		this.promptTemplate = loadPromptTemplate();
	}

	@Override
	public MetadataExtractionData extract(DocumentEntity document) {
		if (apiKey.isBlank()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La cle Gemini n'est pas configuree.");
		}

		PreparedDocument preparedDocument = textPreparationService.prepare(document.getExtractedText());
		String prompt = promptTemplate + "\n<document>\n" + preparedDocument.segmentedExcerpt() + "\n</document>";

		GeminiRequest request = new GeminiRequest(
				List.of(new GeminiContent(List.of(new GeminiPart(prompt)))),
				new GenerationConfig("application/json")
		);

		JsonNode response = restClient.post()
				.uri("/v1beta/models/{model}:generateContent", modelName())
				.header("x-goog-api-key", apiKey)
				.body(request)
				.retrieve()
				.body(JsonNode.class);

		return parseResponse(document, response);
	}

	private String loadPromptTemplate() {
		try {
			return new ClassPathResource("prompts/" + PROMPT_VERSION + ".txt")
					.getContentAsString(StandardCharsets.UTF_8)
					.trim();
		} catch (Exception exception) {
			throw new IllegalStateException("Le prompt d'extraction est introuvable.", exception);
		}
	}

	private MetadataExtractionData parseResponse(DocumentEntity document, JsonNode response) {
		JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
		StringBuilder builder = new StringBuilder();
		if (parts.isArray()) {
			for (JsonNode part : parts) {
				builder.append(part.path("text").asText(""));
			}
		}
		String text = builder.toString();

		if (text.isBlank()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La reponse Gemini est vide.");
		}

		try {
			String cleanedText = text.replace("```json", "").replace("```", "").trim();
			JsonNode metadata = objectMapper.readTree(cleanedText);
			// Un champ absent reste vide : il sera signale au bibliothecaire au lieu d'etre invente.
			return new MetadataExtractionData(
					textOrNull(metadata, "title"),
					authors(metadata),
					textOrNull(metadata, "summary"),
					textOrNull(metadata, "classification"),
					keywords(metadata.path("keywords")),
					textOrNull(metadata, "publication_date"),
					doi(textOrNull(metadata, "doi")),
					confidences(metadata.path("confidences"))
			);
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La reponse Gemini n'est pas exploitable.");
		}
	}

	@Override
	/** Modele choisi par l'administrateur (configuration A2), sinon celui du deploiement. */
	public String modelName() {
		return settings == null ? model : settings.llmModel();
	}

	@Override
	public String promptVersion() {
		return PROMPT_VERSION;
	}

	/** Valeur textuelle du champ, ou null s'il est absent, vide ou explicitement null. */
	private String textOrNull(JsonNode metadata, String field) {
		JsonNode node = metadata.path(field);
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		String value = node.asText("").trim();
		return value.isEmpty() ? null : value;
	}

	/** Retire le prefixe resolveur si le modele l'a ajoute malgre la consigne. */
	private String doi(String value) {
		if (value == null) {
			return null;
		}
		String cleaned = value
				.replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "")
				.replaceFirst("(?i)^doi:\\s*", "")
				.trim();
		return cleaned.isEmpty() ? null : cleaned;
	}

	/**
	 * Auteurs joints par des virgules, format attendu par la suite de la chaine.
	 * Le prompt v5 demande une liste ; une virgule interne ("Nom, Prenom") casserait le
	 * decoupage a la validation, elle est donc remplacee par une espace.
	 */
	private String authors(JsonNode metadata) {
		JsonNode list = metadata.path("authors");
		if (!list.isArray()) {
			return textOrNull(metadata, "author");
		}
		String joined = StreamSupport.stream(list.spliterator(), false)
				.map(JsonNode::asText)
				.map(name -> name.replace(',', ' ').replaceAll("\\s+", " ").trim())
				.map(this::withoutCapitalizedWords)
				.filter(name -> !name.isBlank())
				.collect(Collectors.joining(", "));
		return joined.isEmpty() ? null : joined;
	}

	/**
	 * Beaucoup de pages de titre ecrivent le nom de famille en capitales ("Vincent BRUNIN") :
	 * le catalogue l'affiche en casse normale. Les initiales ("J.") restent intactes.
	 */
	private String withoutCapitalizedWords(String name) {
		return Arrays.stream(name.split(" "))
				.map(word -> word.chars().filter(Character::isLetter).count() > 1 && word.equals(word.toUpperCase(Locale.ROOT))
						? capitalizeParts(word.toLowerCase(Locale.ROOT))
						: word)
				.collect(Collectors.joining(" "));
	}

	private String capitalizeParts(String word) {
		StringBuilder result = new StringBuilder(word.length());
		boolean startOfPart = true;
		for (char character : word.toCharArray()) {
			result.append(startOfPart ? Character.toUpperCase(character) : character);
			startOfPart = character == '-' || character == '\'' || character == '\u2019';
		}
		return result.toString();
	}

	private List<String> keywords(JsonNode node) {
		if (!node.isArray()) {
			return List.of();
		}
		return StreamSupport.stream(node.spliterator(), false)
				.map(JsonNode::asText)
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.limit(12)
				.toList();
	}

	/**
	 * Confiances annoncees par le modele, ramenees a l'intervalle [0,1].
	 * Une entree non numerique ou hors bornes est ignoree plutot que corrigee silencieusement.
	 */
	private Map<String, Double> confidences(JsonNode node) {
		if (!node.isObject()) {
			return Map.of();
		}
		Map<String, Double> confidences = new LinkedHashMap<>();
		node.properties().forEach(entry -> {
			JsonNode value = entry.getValue();
			if (value.isNumber()) {
				double confidence = value.asDouble();
				if (confidence >= 0.0 && confidence <= 1.0) {
					confidences.put(entry.getKey(), confidence);
				}
			}
		});
		return Map.copyOf(confidences);
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

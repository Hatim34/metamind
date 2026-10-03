package be.icc.metamind.extraction;

import java.util.List;
import java.nio.charset.StandardCharsets;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentEntity;

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
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;
	private final TextPreparationService textPreparationService;
	private final String promptTemplate;

	public GeminiMetadataExtractionProvider(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			@Value("${metamind.gemini.api-key:}") String apiKey,
			@Value("${metamind.gemini.model:gemini-3.5-flash-lite}") String model,
			TextPreparationService textPreparationService
	) {
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
				.uri("/v1beta/models/{model}:generateContent", model)
				.header("x-goog-api-key", apiKey)
				.body(request)
				.retrieve()
				.body(JsonNode.class);

		return parseResponse(document, response);
	}

	private String loadPromptTemplate() {
		try {
			return new ClassPathResource("prompts/extraction-v2.txt")
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
			return new MetadataExtractionData(
					metadata.path("title").asText(document.getFileName()),
					metadata.path("author").asText("Auteur non renseigne"),
					metadata.path("summary").asText("Resume a valider par le bibliothecaire."),
					metadata.path("classification").asText("Publication scientifique"),
					keywords(metadata.path("keywords"))
			);
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La reponse Gemini n'est pas exploitable.");
		}
	}

	@Override
	public String modelName() {
		return model;
	}

	private List<String> keywords(JsonNode node) {
		if (!node.isArray()) {
			return List.of("publication", "validation", "bibliotheque");
		}
		return java.util.stream.StreamSupport.stream(node.spliterator(), false)
				.map(JsonNode::asText)
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.limit(8)
				.toList();
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

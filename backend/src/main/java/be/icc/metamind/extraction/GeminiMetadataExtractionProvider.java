package be.icc.metamind.extraction;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.publication.PublicationEntity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "gemini")
public class GeminiMetadataExtractionProvider implements MetadataExtractionProvider {
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	public GeminiMetadataExtractionProvider(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			@Value("${metamind.gemini.api-key:}") String apiKey,
			@Value("${metamind.gemini.model:gemini-2.5-flash-lite}") String model
	) {
		this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public MetadataExtractionData extract(PublicationEntity publication) {
		if (apiKey.isBlank()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La cle Gemini n'est pas configuree.");
		}

		String prompt = """
				Extrais des metadonnees Dublin Core depuis cette publication.
				Reponds uniquement en JSON avec les champs title, author et keywords.
				Titre existant : %s
				Auteur existant : %s
				Annee : %d
				Mots-cles existants : %s
				""".formatted(publication.getTitle(), publication.getAuthor(), publication.getYear(), publication.getKeywordsText());

		GeminiRequest request = new GeminiRequest(List.of(new GeminiContent(List.of(new GeminiPart(prompt)))));

		JsonNode response = restClient.post()
				.uri("/v1beta/models/{model}:generateContent?key={apiKey}", model, apiKey)
				.body(request)
				.retrieve()
				.body(JsonNode.class);

		return parseResponse(publication, response);
	}

	private MetadataExtractionData parseResponse(PublicationEntity publication, JsonNode response) {
		String text = response.path("candidates")
				.path(0)
				.path("content")
				.path("parts")
				.path(0)
				.path("text")
				.asText("");

		if (text.isBlank()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La reponse Gemini est vide.");
		}

		try {
			String cleanedText = text.replace("```json", "").replace("```", "").trim();
			JsonNode metadata = objectMapper.readTree(cleanedText);
			return new MetadataExtractionData(
					metadata.path("title").asText(publication.getTitle()),
					metadata.path("author").asText(publication.getAuthor()),
					keywords(metadata.path("keywords"))
			);
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "La reponse Gemini n'est pas exploitable.");
		}
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

	private record GeminiRequest(List<GeminiContent> contents) {
	}

	private record GeminiContent(List<GeminiPart> parts) {
	}

	private record GeminiPart(String text) {
	}
}

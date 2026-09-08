package be.icc.metamind.opendata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import be.icc.metamind.document.DocumentAuthorEntity;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.MetadataEntity;

@Service
public class DspacePublisher {
	private static final Logger log = LoggerFactory.getLogger(DspacePublisher.class);
	private final RestClient.Builder restClientBuilder;
	private final boolean enabled;
	private final String baseUrl;
	private final String token;

	public DspacePublisher(RestClient.Builder restClientBuilder,
			@Value("${metamind.dspace.enabled:false}") boolean enabled,
			@Value("${metamind.dspace.url:}") String baseUrl,
			@Value("${metamind.dspace.token:}") String token) {
		this.restClientBuilder = restClientBuilder;
		this.enabled = enabled;
		this.baseUrl = baseUrl;
		this.token = token;
	}

	public void publish(DocumentEntity document, MetadataEntity metadata, List<DocumentAuthorEntity> authors, List<String> keywords) {
		if (!enabled || baseUrl.isBlank() || token.isBlank()) {
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", metadata.getTitre());
		payload.put("type", "item");
		payload.put("metadata", Map.of(
				"dc.title", List.of(Map.of("value", metadata.getTitre(), "language", "fr")),
				"dc.description", List.of(Map.of("value", metadata.getResume() == null ? "" : metadata.getResume(), "language", "fr")),
				"dc.date.issued", List.of(Map.of("value", metadata.getPublicationDate() == null ? "" : metadata.getPublicationDate().toString())),
				"dc.contributor.author", authors.stream().map(author -> Map.of("value", author.getAuthor().getFullName())).toList(),
				"dc.subject", keywords.stream().map(keyword -> Map.of("value", keyword)).toList()
		));
		try {
			restClientBuilder.build().post()
					.uri(baseUrl.replaceAll("/$", "") + "/api/core/items")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
		} catch (RuntimeException exception) {
			log.warn("La publication DSpace a echoue pour le document {}.", document.getId());
		}
	}
}

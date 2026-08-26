package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiMetadataExtractionProviderTests {
	private final InstitutionEntity institution = new InstitutionEntity("INST-A", "Institution A", "institution-a.example");

	@Test
	void extractsMetadataFromGeminiResponse() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		GeminiMetadataExtractionProvider provider = new GeminiMetadataExtractionProvider(
				restClientBuilder,
				new ObjectMapper(),
				"secret",
				"gemini-test"
		);
		String response = """
				{
				  "candidates": [
				    {
				      "content": {
				        "parts": [
				          {
				            "text": "{\\"title\\":\\"Titre enrichi\\",\\"author\\":\\"Mina Laurent\\",\\"keywords\\":[\\"Dublin Core\\",\\"catalogage\\"]}"
				          }
				        ]
				      }
				    }
				  ]
				}
				""";
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent?key=secret"))
				.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		MetadataExtractionData metadata = provider.extract(publication());

		assertThat(metadata.title()).isEqualTo("Titre enrichi");
		assertThat(metadata.author()).isEqualTo("Mina Laurent");
		assertThat(metadata.keywords()).containsExactly("Dublin Core", "catalogage");
		server.verify();
	}

	@Test
	void refusesExtractionWhenApiKeyIsMissing() {
		GeminiMetadataExtractionProvider provider = new GeminiMetadataExtractionProvider(
				RestClient.builder(),
				new ObjectMapper(),
				"",
				"gemini-test"
		);

		assertThatThrownBy(() -> provider.extract(publication()))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("Gemini");
	}

	private PublicationEntity publication() {
		return new PublicationEntity(
				"Analyse automatique des metadonnees",
				"Sarah Lemaire",
				2026,
				PublicationStatus.EN_ATTENTE,
				Visibility.PUBLIC,
				"catalogage",
				institution
		);
	}
}

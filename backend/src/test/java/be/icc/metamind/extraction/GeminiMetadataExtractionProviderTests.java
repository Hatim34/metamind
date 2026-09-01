package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.institution.InstitutionEntity;

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
				            "text": "{\\"title\\":\\"Titre enrichi\\",\\"author\\":\\"Mina Laurent\\",\\"summary\\":\\"Resume structure\\",\\"classification\\":\\"Sciences de l'information\\",\\"keywords\\":[\\"Dublin Core\\",\\"catalogage\\"]}"
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
		assertThat(metadata.summary()).isEqualTo("Resume structure");
		assertThat(metadata.classification()).isEqualTo("Sciences de l'information");
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

	private DocumentEntity publication() {
		return new DocumentEntity(
				"analyse-automatique-des-metadonnees.txt",
				null,
				0L,
				"TXT",
				"Analyse automatique des metadonnees",
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.PUBLIC,
				institution,
				null
		);
	}
}

package be.icc.metamind.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiPublicationTranslationProviderTests {
	@Test
	void translatesOnlyTheDescriptiveFields() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		GeminiPublicationTranslationProvider provider = new GeminiPublicationTranslationProvider(
				restClientBuilder, new ObjectMapper(), "gemini-test", "gemini-test"
		);
		server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"))
				.andExpect(header("x-goog-api-key", "gemini-test"))
				.andRespond(withSuccess("""
						{
						  "candidates": [{"content": {"parts": [{"text": "{\\"title\\":\\"Water management in Belgian cities\\",\\"summary\\":\\"A study of local policies.\\",\\"keywords\\":[\\"water\\",\\"cities\\"]}"}]}}]
						}
						""", MediaType.APPLICATION_JSON));

		PublicationTranslation result = provider.translate(
				new TranslationSource("Gestion de l'eau", "Une etude des politiques locales.", List.of("eau", "villes")),
				"fr", "en"
		);

		assertThat(result.title()).isEqualTo("Water management in Belgian cities");
		assertThat(result.summary()).isEqualTo("A study of local policies.");
		assertThat(result.keywords()).containsExactly("water", "cities");
		assertThat(result.translated()).isTrue();
		server.verify();
	}
}

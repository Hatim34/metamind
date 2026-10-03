package be.icc.metamind.document;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@Transactional
class ReferenceDataControllerTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LanguageRepository languageRepository;

	@Autowired
	private DocumentTypeRepository documentTypeRepository;

	@BeforeEach
	void seedReferenceData() {
		if (languageRepository.findByCodeIgnoreCase("fr").isEmpty()) {
			languageRepository.save(new LanguageEntity("fr", "Francais"));
		}
		if (documentTypeRepository.findByCodeIgnoreCase("these").isEmpty()) {
			documentTypeRepository.save(new DocumentTypeEntity("these", "These"));
		}
	}

	@Test
	void exposesTheControlledVocabulariesWithoutAToken() throws Exception {
		mockMvc.perform(get("/api/v1/references"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.langues[?(@.code == 'fr')].libelle").exists())
				.andExpect(jsonPath("$.types_documents[?(@.code == 'these')].libelle").exists());
	}
}

package be.icc.metamind.opendata;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.support.TestDocumentFactory;

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
class OpenDataControllerTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private MetadataRepository metadataRepository;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private DocumentAuthorRepository documentAuthorRepository;

	@Autowired
	private DocumentKeywordRepository documentKeywordRepository;

	private DocumentEntity publicPublication;
	private DocumentEntity restrictedPublication;

	@BeforeEach
	void setUp() {
		InstitutionEntity institution = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		TestDocumentFactory documents = new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository);
		publicPublication = documents.create(
				"Analyse automatique des metadonnees",
				"Sarah Lemaire",
				2026,
				DocumentStatus.PUBLIE,
				DocumentVisibility.PUBLIC,
				List.of("Dublin Core", "metadonnees"),
				institution,
				null
		);
		restrictedPublication = documents.create(
				"Rapport interne reserve",
				"Sarah Lemaire",
				2026,
				DocumentStatus.A_VALIDER,
				DocumentVisibility.INSTITUTION,
				List.of("catalogage"),
				institution,
				null
		);
	}

	@Test
	void rssFeedContainsOnlyPublicPublishedPublications() throws Exception {
		String response = mockMvc.perform(get("/api/v1/open-data/rss"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).contains("<rss version=\"2.0\">");
		assertThat(response).contains("Analyse automatique des metadonnees");
		assertThat(response).doesNotContain("Rapport interne reserve");
	}

	@Test
	void dublinCoreExportContainsAcademicMetadata() throws Exception {
		String response = mockMvc.perform(get("/api/v1/open-data/publications/" + publicPublication.getId() + "/dublin-core"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/xml"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).contains("xmlns:dc=\"http://purl.org/dc/elements/1.1/\"");
		assertThat(response).contains("<dc:title>Analyse automatique des metadonnees</dc:title>");
		assertThat(response).contains("<dc:creator>Sarah Lemaire</dc:creator>");
		assertThat(response).contains("<dc:subject>Dublin Core</dc:subject>");
	}

	@Test
	void dublinCoreExportRejectsRestrictedPublication() throws Exception {
		mockMvc.perform(get("/api/v1/open-data/publications/" + restrictedPublication.getId() + "/dublin-core"))
				.andExpect(status().isNotFound());
	}
}

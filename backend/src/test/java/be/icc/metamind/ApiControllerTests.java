package be.icc.metamind;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;
import be.icc.metamind.user.PasswordService;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublicationRepository publicationRepository;

	@Autowired
	private PasswordService passwordService;

	@BeforeEach
	void setUp() {
		if (userRepository.existsByEmailIgnoreCase("sarah@institution-a.example")) {
			return;
		}

		InstitutionEntity institution = institutionRepository.findByCodeIgnoreCase("INST-A")
				.orElseGet(() -> institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example")));
		userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("558435"),
				UserRole.BIBLIOTHECAIRE,
				institution
		));
		publicationRepository.save(new PublicationEntity(
				"Analyse automatique des metadonnees pour les depots institutionnels",
				"Sarah Lemaire",
				2026,
				PublicationStatus.PUBLIE,
				Visibility.PUBLIC,
				"Dublin Core,metadonnees,recherche",
				institution
		));
	}

	@Test
	void healthReturnsUpStatus() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")));
	}

	@Test
	void publicationsCanBeListedAndFiltered() throws Exception {
		mockMvc.perform(get("/api/v1/publications").param("search", "Dublin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status", is("PUBLIE")));
	}

	@Test
	void publicationCanBeCreated() throws Exception {
		String body = """
				{
				  "title": "Controle qualite des metadonnees importees",
				  "author": "Mina Laurent",
				  "institution": "Institution A",
				  "year": 2026,
				  "visibility": "INSTITUTION",
				  "keywords": ["qualite", "catalogage"]
				}
				""";

		mockMvc.perform(post("/api/v1/publications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title", is("Controle qualite des metadonnees importees")))
				.andExpect(jsonPath("$.status", is("A_VALIDER")))
				.andExpect(jsonPath("$.visibility", is("INSTITUTION")));
	}

	@Test
	void librarianCanLogin() throws Exception {
		String body = """
				{
				  "email": "sarah@institution-a.example",
				  "password": "558435"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", is("token-alpha-1")))
				.andExpect(jsonPath("$.user.role", is("Bibliothecaire")));
	}

	@Test
	void profileCanBeUpdated() throws Exception {
		String body = """
				{
				  "firstName": "Sarah",
				  "lastName": "Lemaire",
				  "institution": "Institution A"
				}
				""";

		mockMvc.perform(put("/api/v1/users/1/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.institution", is("Institution A")));
	}

	@Test
	void accountDeletionUsesSoftDeleteStatus() throws Exception {
		mockMvc.perform(delete("/api/v1/users/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("DESACTIVE")));
	}
}

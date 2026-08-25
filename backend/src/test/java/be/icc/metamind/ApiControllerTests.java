package be.icc.metamind;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@Transactional
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
		InstitutionEntity otherInstitution = institutionRepository.findByCodeIgnoreCase("INST-B")
				.orElseGet(() -> institutionRepository.save(new InstitutionEntity("INST-B", "Institution B", "institution-b.example")));
		userRepository.save(new UserEntity(
				"Sarah",
				"Lemaire",
				"sarah@institution-a.example",
				passwordService.hash("558435"),
				UserRole.BIBLIOTHECAIRE,
				institution
		));
		userRepository.save(new UserEntity(
				"Jan",
				"Peeters",
				"jan@institution-b.example",
				passwordService.hash("558435"),
				UserRole.BIBLIOTHECAIRE,
				otherInstitution
		));
		userRepository.save(new UserEntity(
				"Admin",
				"Metamind",
				"admin@metamind.example",
				passwordService.hash("558435"),
				UserRole.ADMINISTRATEUR,
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
		publicationRepository.save(new PublicationEntity(
				"Rapport interne reserve a l institution",
				"Sarah Lemaire",
				2026,
				PublicationStatus.A_VALIDER,
				Visibility.INSTITUTION,
				"catalogage,validation",
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
						.header("Authorization", bearerToken())
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
				.andExpect(jsonPath("$.token", startsWith("eyJ")))
				.andExpect(jsonPath("$.user.role", is("Bibliothecaire")));
	}

	@Test
	void profileCanBeUpdated() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		String body = """
				{
				  "firstName": "Sarah",
				  "lastName": "Lemaire",
				  "institution": "Institution A"
				}
				""";

		mockMvc.perform(put("/api/v1/users/" + user.getId() + "/profile")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.institution", is("Institution A")));
	}

	@Test
	void accountDeletionUsesSoftDeleteStatus() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();

		mockMvc.perform(delete("/api/v1/users/" + user.getId())
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("DESACTIVE")));
	}

	@Test
	void creditsAndExtractionWorkflowIsSecuredAndConsumesOneCredit() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		PublicationEntity publication = publicationRepository.findAll().getFirst();
		String authorization = bearerToken();

		mockMvc.perform(post("/api/v1/users/" + user.getId() + "/credits/purchase")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance", is(3)));

		mockMvc.perform(post("/api/v1/publications/" + publication.getId() + "/extraction")
						.header("Authorization", authorization))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publicationId", is(publication.getId().intValue())))
				.andExpect(jsonPath("$.creditBalance", is(2)))
				.andExpect(jsonPath("$.suggestedKeywords", hasSize(3)));
	}

	@Test
	void institutionOnlyPublicationIsHiddenFromOtherInstitutions() throws Exception {
		PublicationEntity restrictedPublication = publicationRepository.findAll().stream()
				.filter(publication -> publication.getVisibility() == Visibility.INSTITUTION)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(get("/api/v1/publications/" + restrictedPublication.getId()))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/publications/" + restrictedPublication.getId())
						.header("Authorization", bearerToken("jan@institution-b.example", "558435")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/publications/" + restrictedPublication.getId())
						.header("Authorization", bearerToken("sarah@institution-a.example", "558435")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility", is("INSTITUTION")));

		mockMvc.perform(get("/api/v1/publications/" + restrictedPublication.getId())
						.header("Authorization", bearerToken("admin@metamind.example", "558435")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility", is("INSTITUTION")));
	}

	@Test
	void publicationStatusCanBeUpdatedByInstitutionLibrarian() throws Exception {
		PublicationEntity publication = publicationRepository.findAll().stream()
				.filter(item -> item.getStatus() == PublicationStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"PUBLIE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("PUBLIE")));
	}

	@Test
	void publicationStatusRejectsOtherInstitutionLibrarian() throws Exception {
		PublicationEntity publication = publicationRepository.findAll().stream()
				.filter(item -> item.getStatus() == PublicationStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken("jan@institution-b.example", "558435"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"PUBLIE\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicationStatusRejectsInternalProcessingStatus() throws Exception {
		PublicationEntity publication = publicationRepository.findAll().stream()
				.filter(item -> item.getStatus() == PublicationStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"EXTRACTION\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void librarianStatisticsAreLimitedToInstitution() throws Exception {
		mockMvc.perform(get("/api/v1/statistics")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope", is("Institution A")))
				.andExpect(jsonPath("$.totalPublications", is(2)))
				.andExpect(jsonPath("$.publishedPublications", is(1)))
				.andExpect(jsonPath("$.pendingValidationPublications", is(1)))
				.andExpect(jsonPath("$.institutionOnlyPublications", is(1)));
	}

	@Test
	void adminStatisticsAreGlobal() throws Exception {
		InstitutionEntity otherInstitution = institutionRepository.findByCodeIgnoreCase("INST-B").orElseThrow();
		publicationRepository.save(new PublicationEntity(
				"Corpus institutionnel reserve",
				"Jan Peeters",
				2026,
				PublicationStatus.PUBLIE,
				Visibility.INSTITUTION,
				"corpus,recherche",
				otherInstitution
		));

		mockMvc.perform(get("/api/v1/statistics")
						.header("Authorization", bearerToken("admin@metamind.example", "558435")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope", is("GLOBAL")))
				.andExpect(jsonPath("$.totalPublications", is(3)))
				.andExpect(jsonPath("$.publishedPublications", is(2)))
				.andExpect(jsonPath("$.institutionOnlyPublications", is(2)));
	}

	@Test
	void protectedProfileRejectsMissingToken() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();

		mockMvc.perform(get("/api/v1/users/" + user.getId() + "/profile"))
				.andExpect(status().isUnauthorized());
	}

	private String bearerToken() throws Exception {
		return bearerToken("sarah@institution-a.example", "558435");
	}

	private String bearerToken(String email, String password) throws Exception {
		String body = """
				{
				  "email": "%s",
				  "password": "%s"
				}
				""".formatted(email, password);
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = response.replaceFirst(".*\\\"token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
		return "Bearer " + token;
	}
}

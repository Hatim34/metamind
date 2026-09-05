package be.icc.metamind.institution;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class InstitutionControllerTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InstitutionRepository repository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordService passwordService;

	@BeforeEach
	void setUp() {
		InstitutionEntity platform = repository.findByCodeIgnoreCase("META")
				.orElseGet(() -> repository.save(new InstitutionEntity("META", "Metamind", "metamind.example")));
		if (!userRepository.existsByEmailIgnoreCase("admin@metamind.example")) {
			userRepository.save(new UserEntity(
					"Nadia",
					"Benali",
					"admin@metamind.example",
					passwordService.hash("558435"),
					UserRole.ADMIN,
					platform
			));
		}
	}

	@Test
	void createsAndListsInstitution() throws Exception {
		String body = """
				{
				  "code": "INST-A",
				  "name": "Institution A",
				  "emailDomain": "institution-a.example"
				}
				""";

		mockMvc.perform(post("/api/v1/institutions")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code", is("INST-A")))
				.andExpect(jsonPath("$.actif", is(true)));

		mockMvc.perform(get("/api/v1/institutions")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void rejectsDuplicatedInstitutionCode() throws Exception {
		String body = """
				{
				  "code": "INST-B",
				  "name": "Institution B",
				  "emailDomain": "institution-b.example"
				}
				""";

		mockMvc.perform(post("/api/v1/institutions")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/institutions")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void deactivatesInstitution() throws Exception {
		InstitutionEntity institution = repository.save(new InstitutionEntity("INST-C", "Institution C", "institution-c.example"));

		mockMvc.perform(delete("/api/v1/institutions/" + institution.getId())
				.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.actif", is(false)));
	}

	private String bearerToken() throws Exception {
		String body = """
				{
				  "email": "admin@metamind.example",
				  "password": "558435"
				}
				""";
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

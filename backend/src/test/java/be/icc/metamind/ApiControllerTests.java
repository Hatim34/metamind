package be.icc.metamind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.nio.charset.StandardCharsets;

import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataStatus;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.support.TestDocumentFactory;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"metamind.storage.documents-dir=target/test-storage/documents"
})
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

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private CreditMovementRepository creditMovementRepository;

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
				UserRole.LIBRARIAN,
				institution
		));
		userRepository.save(new UserEntity(
				"Jan",
				"Peeters",
				"jan@institution-b.example",
				passwordService.hash("558435"),
				UserRole.LIBRARIAN,
				otherInstitution
		));
		userRepository.save(new UserEntity(
				"Admin",
				"Metamind",
				"admin@metamind.example",
				passwordService.hash("558435"),
				UserRole.ADMIN,
				institution
		));
		TestDocumentFactory documents = new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository);
		documents.create(
				"Analyse automatique des metadonnees pour les depots institutionnels",
				"Sarah Lemaire",
				2026,
				DocumentStatus.PUBLIE,
				DocumentVisibility.PUBLIC,
				List.of("Dublin Core", "metadonnees", "recherche"),
				institution,
				null
		);
		documents.create(
				"Rapport interne reserve a l institution",
				"Sarah Lemaire",
				2026,
				DocumentStatus.A_VALIDER,
				DocumentVisibility.INSTITUTION,
				List.of("catalogage", "validation"),
				institution,
				null
		);
	}

	@Test
	void healthReturnsUpStatus() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")));
	}

	@Test
	void openApiDocumentationIsServed() throws Exception {
		mockMvc.perform(get("/api/v1/openapi.yaml"))
				.andExpect(status().isOk())
				.andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
						.contains("openapi: 3.0.3")
						.contains("/api/v1"));
	}

	@Test
	void spaRoutesAreForwardedToIndex() throws Exception {
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
	}

	@Test
	void spaFallbackDoesNotInterceptApiRoutes() throws Exception {
		mockMvc.perform(get("/api/v1/route-inconnue"))
				.andExpect(status().isNotFound());
	}

	@Test
	void publicationsCanBeListedAndFiltered() throws Exception {
		mockMvc.perform(get("/api/v1/publications").param("search", "Dublin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].statut", is("PUBLIE")));
	}

	@Test
	void documentsListIsPaginatedAndScopedByInstitution() throws Exception {
		InstitutionEntity otherInstitution = institutionRepository.findByNameIgnoreCase("Institution B").orElseThrow();
		TestDocumentFactory documents = new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository);
		documents.create(
				"Document reserve institution B",
				"Jan Peeters",
				2026,
				DocumentStatus.A_VALIDER,
				DocumentVisibility.INSTITUTION,
				List.of("catalogage"),
				otherInstitution,
				null
		);

		mockMvc.perform(get("/api/v1/documents")
						.param("page", "0")
						.param("size", "20")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu", hasSize(2)))
				.andExpect(jsonPath("$.page", is(0)))
				.andExpect(jsonPath("$.size", is(20)))
				.andExpect(jsonPath("$.total_elements", is(2)))
				.andExpect(jsonPath("$.contenu[0].institution", is("Institution A")))
				.andExpect(jsonPath("$.contenu[1].institution", is("Institution A")));
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
				.andExpect(jsonPath("$.titre", is("Controle qualite des metadonnees importees")))
				.andExpect(jsonPath("$.statut", is("A_VALIDER")))
				.andExpect(jsonPath("$.visibilite", is("INSTITUTION")));
	}

	@Test
	void documentCanBeImportedFromTextFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"fichier",
				"article-metadonnees.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"Article scientifique sur les metadonnees Dublin Core.".getBytes(StandardCharsets.UTF_8)
		);

		mockMvc.perform(multipart("/api/v1/documents")
						.file(file)
						.header("Authorization", bearerToken()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.titre", is("article metadonnees")))
				.andExpect(jsonPath("$.institution", is("Institution A")))
				.andExpect(jsonPath("$.statut", is("EN_ATTENTE")))
				.andExpect(jsonPath("$.visibilite", is("INSTITUTION")));

		DocumentEntity document = documentRepository.findAll().stream()
				.filter(item -> "article-metadonnees.txt".equals(item.getFileName()))
				.findFirst()
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(document.getExtractedText()).contains("Dublin Core");
		org.assertj.core.api.Assertions.assertThat(document.getInstitution().getName()).isEqualTo("Institution A");
		org.assertj.core.api.Assertions.assertThat(document.getImportedBy().getEmail()).isEqualTo("sarah@institution-a.example");
	}

	@Test
	void documentImportStoresCoverImage() throws Exception {
		byte[] imageContent = new byte[] { 1, 2, 3, 4 };
		MockMultipartFile file = new MockMultipartFile(
				"fichier",
				"rapport-couverture.txt",
				MediaType.TEXT_PLAIN_VALUE,
				"Rapport scientifique avec couverture.".getBytes(StandardCharsets.UTF_8)
		);
		MockMultipartFile image = new MockMultipartFile(
				"image",
				"couverture.png",
				MediaType.IMAGE_PNG_VALUE,
				imageContent
		);

		mockMvc.perform(multipart("/api/v1/documents")
						.file(file)
						.file(image)
						.header("Authorization", bearerToken()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.image_url", startsWith("/api/v1/documents/")));

		DocumentEntity document = documentRepository.findAll().stream()
				.filter(item -> "rapport-couverture.txt".equals(item.getFileName()))
				.findFirst()
				.orElseThrow();

		mockMvc.perform(get("/api/v1/documents/" + document.getId() + "/image")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
				.andExpect(content().bytes(imageContent));
	}

	@Test
	void documentImportRejectsUnsupportedFormat() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"fichier",
				"archive.exe",
				"application/octet-stream",
				"contenu".getBytes(StandardCharsets.UTF_8)
		);

		mockMvc.perform(multipart("/api/v1/documents")
						.file(file)
						.header("Authorization", bearerToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Le format du fichier doit etre PDF, DOCX ou TXT.")));
	}

	@Test
	void publicationCreationRejectsInvalidRequest() throws Exception {
		String body = """
				{
				  "title": "",
				  "author": "A",
				  "institution": "Institution A",
				  "year": 1800,
				  "visibility": null,
				  "keywords": []
				}
				""";

		mockMvc.perform(post("/api/v1/publications")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("Les donnees envoyees ne sont pas valides.")));
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
				.andExpect(jsonPath("$.expires_in", is(3600)))
				.andExpect(jsonPath("$.utilisateur.role", is("LIBRARIAN")));
	}

	@Test
	void repeatedInvalidLoginAttemptsAreBlocked() throws Exception {
		String body = """
				{
				  "email": "compte-bloque@institution-a.example",
				  "password": "558435000"
				}
				""";

		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(post("/api/v1/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void registerRejectsEmailFromAnotherInstitutionDomain() throws Exception {
		String body = """
				{
				  "firstName": "Mina",
				  "lastName": "Laurent",
				  "email": "mina@institution-b.example",
				  "institution": "Institution A",
				  "password": "55843500"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("L'email ne correspond pas au domaine de l'institution.")));
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
	void profileUpdateRejectsInstitutionWithDifferentEmailDomain() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		String body = """
				{
				  "firstName": "Sarah",
				  "lastName": "Lemaire",
				  "institution": "Institution B"
				}
				""";

		mockMvc.perform(put("/api/v1/users/" + user.getId() + "/profile")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", is("L'email ne correspond pas au domaine de l'institution.")));
	}

	@Test
	void accountDeletionUsesSoftDeleteStatus() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();

		mockMvc.perform(delete("/api/v1/users/" + user.getId())
				.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut", is("DESACTIVE")))
				.andExpect(jsonPath("$.prenom", is("Compte")))
				.andExpect(jsonPath("$.nom", is("Supprime")))
				.andExpect(jsonPath("$.email", is("compte-supprime-" + user.getId() + "@metamind.example")));
	}

	@Test
	void creditCheckoutRequiresPaymentConfirmation() throws Exception {
		String body = """
				{
				  "pack_id": 2,
				  "cgv_acceptees": true
				}
				""";

		String response = mockMvc.perform(post("/api/v1/credits")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkout_url", startsWith("https://metamind-app.duckdns.org/paiement/confirmation")))
				.andExpect(jsonPath("$.reference", startsWith("pay_")))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String reference = response.replaceFirst(".*\\\"reference\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
		assertThat(creditMovementRepository.count()).isZero();

		mockMvc.perform(post("/api/v1/webhooks/stripe")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reference\":\"" + reference + "\",\"type\":\"checkout.session.completed\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.solde_credits", is(100)));
	}

	@Test
	void stripeWebhookAcceptsCheckoutSessionEventPayload() throws Exception {
		String response = mockMvc.perform(post("/api/v1/credits")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"pack_id\":2,\"cgv_acceptees\":true}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String reference = response.replaceFirst(".*\\\"reference\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
		String event = """
				{
				  "type": "checkout.session.completed",
				  "data": {
				    "object": {
				      "client_reference_id": "%s"
				    }
				  }
				}
				""".formatted(reference);

		mockMvc.perform(post("/api/v1/webhooks/stripe")
						.contentType(MediaType.APPLICATION_JSON)
						.content(event))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.solde_credits", is(100)));
	}

	@Test
	void administratorCanAdjustInstitutionCredits() throws Exception {
		InstitutionEntity institution = institutionRepository.findByNameIgnoreCase("Institution A").orElseThrow();
		String body = """
				{
				  "amount": 30,
				  "reason": "Correction administrative"
				}
				""";

		mockMvc.perform(post("/api/v1/admin/institutions/" + institution.getId() + "/credits/adjustments")
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.solde_credits", is(30)));
	}

	@Test
	void publicSearchReturnsOnlyPublishedPublicDocuments() throws Exception {
		mockMvc.perform(get("/api/v1/search").param("q", "institution"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu", hasSize(1)))
				.andExpect(jsonPath("$.page", is(0)))
				.andExpect(jsonPath("$.size", is(20)))
				.andExpect(jsonPath("$.total_elements", is(1)))
				.andExpect(jsonPath("$.contenu[0].titre", is("Analyse automatique des metadonnees pour les depots institutionnels")))
				.andExpect(jsonPath("$.contenu[0].visibilite", is("PUBLIC")));
	}

	@Test
	void administratorCanListUsersAndFilterByInstitution() throws Exception {
		InstitutionEntity institution = institutionRepository.findByNameIgnoreCase("Institution B").orElseThrow();

		mockMvc.perform(get("/api/v1/admin/users")
						.param("institutionId", institution.getId().toString())
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu", hasSize(1)))
				.andExpect(jsonPath("$.total_elements", is(1)))
				.andExpect(jsonPath("$.contenu[0].email", is("jan@institution-b.example")));
	}

	@Test
	void administratorCanUpdateUserStatus() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("jan@institution-b.example").orElseThrow();
		String body = """
				{
				  "statut": "DESACTIVE",
				  "role": "LIBRARIAN"
				}
				""";

		mockMvc.perform(patch("/api/v1/admin/users/" + user.getId())
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut", is("DESACTIVE")));
	}

	@Test
	void administratorCanReadConfigurationAndLogs() throws Exception {
		mockMvc.perform(get("/api/v1/admin/config")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prix_credit_eur", is("0.50")))
				.andExpect(jsonPath("$.taille_max_upload_mo", is("128")));

		mockMvc.perform(get("/api/v1/admin/logs")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk());
	}

	@Test
	void creditsAndExtractionWorkflowIsSecuredAndConsumesOneCredit() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		DocumentEntity publication = documentRepository.findAll().getFirst();
		String authorization = bearerToken();

		mockMvc.perform(post("/api/v1/users/" + user.getId() + "/credits/purchase")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.solde_credits", is(3)));

		mockMvc.perform(post("/api/v1/publications/" + publication.getId() + "/extraction")
						.header("Authorization", authorization))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.statut", is("TERMINE")))
				.andExpect(jsonPath("$.publication_id", is(publication.getId().intValue())))
				.andExpect(jsonPath("$.solde_credits", is(2)))
				.andExpect(jsonPath("$.mots_cles_suggeres", hasSize(3)));

		mockMvc.perform(get("/api/v1/users/" + user.getId() + "/credits/movements")
						.header("Authorization", authorization))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].solde_apres", is(2)));
	}

	@Test
	void institutionOnlyPublicationIsHiddenFromOtherInstitutions() throws Exception {
		DocumentEntity restrictedPublication = documentRepository.findAll().stream()
				.filter(publication -> publication.getVisibility() == DocumentVisibility.INSTITUTION)
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
				.andExpect(jsonPath("$.visibilite", is("INSTITUTION")));

		mockMvc.perform(get("/api/v1/publications/" + restrictedPublication.getId())
				.header("Authorization", bearerToken("admin@metamind.example", "558435")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibilite", is("INSTITUTION")));
	}

	@Test
	void publicationStatusCanBeUpdatedByInstitutionLibrarian() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PUBLIE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut", is("PUBLIE")));
	}

	@Test
	void publicationStatusRejectsOtherInstitutionLibrarian() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
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
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"EXTRACTION\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void documentMetadataCanBeReadByInstitutionLibrarian() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(get("/api/v1/documents/" + publication.getId() + "/metadata")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.document_id", is(publication.getId().intValue())))
				.andExpect(jsonPath("$.titre", is("Rapport interne reserve a l institution")))
				.andExpect(jsonPath("$.visibilite", is("INSTITUTION")))
				.andExpect(jsonPath("$.statut", is("EN_ATTENTE")));
	}

	@Test
	void documentMetadataValidationPublishesDocument() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();
		String body = """
				{
				  "titre": "Rapport valide sur le catalogage",
				  "resume": "Resume corrige par le bibliothecaire.",
				  "date_publication": "2026-04-15",
				  "classification": "Sciences de l'information",
				  "visibilite": "PUBLIC",
				  "auteurs": [
				    { "nom_complet": "Sarah Lemaire", "orcid": "0000-0002-1825-0097" },
				    { "nom_complet": "Mina Laurent" }
				  ],
				  "mots_cles": ["catalogage", "Dublin Core", "validation"]
				}
				""";

		mockMvc.perform(put("/api/v1/documents/" + publication.getId() + "/metadata")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titre", is("Rapport valide sur le catalogage")))
				.andExpect(jsonPath("$.visibilite", is("PUBLIC")))
				.andExpect(jsonPath("$.statut", is("VALIDE")))
				.andExpect(jsonPath("$.auteurs", hasSize(2)))
				.andExpect(jsonPath("$.mots_cles", hasSize(3)));

		MetadataEntity metadata = metadataRepository.findByDocumentId(publication.getId()).orElseThrow();
		assertThat(metadata.getStatus()).isEqualTo(MetadataStatus.VALIDE);
		assertThat(metadata.getValidatedBy().getEmail()).isEqualTo("sarah@institution-a.example");
		assertThat(metadata.getValidatedAt()).isNotNull();
		assertThat(publication.getStatus()).isEqualTo(DocumentStatus.PUBLIE);
		assertThat(publication.getVisibility()).isEqualTo(DocumentVisibility.PUBLIC);
	}

	@Test
	void documentMetadataValidationRejectsOtherInstitutionLibrarian() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();
		String body = """
				{
				  "titre": "Tentative externe",
				  "resume": "Controle institutionnel.",
				  "visibilite": "PUBLIC",
				  "auteurs": [{ "nom_complet": "Jan Peeters" }],
				  "mots_cles": ["controle"]
				}
				""";

		mockMvc.perform(put("/api/v1/documents/" + publication.getId() + "/metadata")
						.header("Authorization", bearerToken("jan@institution-b.example", "558435"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicationDeletionUsesSoftDeleteStatus() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getVisibility() == DocumentVisibility.PUBLIC)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(delete("/api/v1/publications/" + publication.getId())
				.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut", is("SUPPRIME")));

		mockMvc.perform(get("/api/v1/publications/" + publication.getId()))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicationDeletionRejectsOtherInstitutionLibrarian() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(delete("/api/v1/publications/" + publication.getId())
						.header("Authorization", bearerToken("jan@institution-b.example", "558435")))
				.andExpect(status().isForbidden());
	}

	@Test
	void librarianStatisticsAreLimitedToInstitution() throws Exception {
		mockMvc.perform(get("/api/v1/stats")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope", is("Institution A")))
				.andExpect(jsonPath("$.total_publications", is(2)))
				.andExpect(jsonPath("$.publications_publiees", is(1)))
				.andExpect(jsonPath("$.publications_a_valider", is(1)))
				.andExpect(jsonPath("$.publications_institution", is(1)));
	}

	@Test
	void adminStatisticsAreGlobal() throws Exception {
		InstitutionEntity otherInstitution = institutionRepository.findByCodeIgnoreCase("INST-B").orElseThrow();
		new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository).create(
				"Corpus institutionnel reserve",
				"Jan Peeters",
				2026,
				DocumentStatus.PUBLIE,
				DocumentVisibility.INSTITUTION,
				List.of("corpus", "recherche"),
				otherInstitution,
				null
		);

		mockMvc.perform(get("/api/v1/stats")
						.header("Authorization", bearerToken("admin@metamind.example", "558435")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scope", is("GLOBAL")))
				.andExpect(jsonPath("$.total_publications", is(3)))
				.andExpect(jsonPath("$.publications_publiees", is(2)))
				.andExpect(jsonPath("$.publications_institution", is(2)));
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

	private String adminBearerToken() throws Exception {
		return bearerToken("admin@metamind.example", "558435");
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

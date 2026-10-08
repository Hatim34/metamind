package be.icc.metamind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import be.icc.metamind.user.UserStatus;

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

	@Autowired
	private be.icc.metamind.publication.PublicationTranslationRepository translationRepository;

	@Autowired
	private be.icc.metamind.publication.PublicationTranslationService translationService;

	@Autowired
	private be.icc.metamind.user.PasswordResetTokenRepository passwordResetTokenRepository;

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
	void securityPolicyAllowsBlobResourcesForProtectedFiles() throws Exception {
		mockMvc.perform(get("/api/v1/references"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Security-Policy", containsString("img-src 'self' data: blob:")))
				.andExpect(header().string("Content-Security-Policy", containsString("frame-src 'self' blob:")));
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
				.andExpect(jsonPath("$.visibilite", is("INSTITUTION")))
				.andExpect(jsonPath("$.fichier_url", startsWith("/api/v1/documents/")));

		DocumentEntity document = documentRepository.findAll().stream()
				.filter(item -> "article-metadonnees.txt".equals(item.getFileName()))
				.findFirst()
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(document.getExtractedText()).isNull();
		org.assertj.core.api.Assertions.assertThat(document.getMimeType()).isEqualTo("TXT");
		org.assertj.core.api.Assertions.assertThat(document.getInstitution().getName()).isEqualTo("Institution A");
		org.assertj.core.api.Assertions.assertThat(document.getImportedBy().getEmail()).isEqualTo("sarah@institution-a.example");

		mockMvc.perform(get("/api/v1/documents/" + document.getId() + "/file")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
				.andExpect(content().string("Article scientifique sur les metadonnees Dublin Core."));
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
	void adminReplacesTheImageOfADocumentUnderANewAddress() throws Exception {
		byte[] firstPage = new byte[] { 1, 2, 3, 4 };
		byte[] figure = new byte[] { 9, 8, 7, 6, 5 };
		String created = mockMvc.perform(multipart("/api/v1/documents")
						.file(new MockMultipartFile("fichier", "article-figure.txt", MediaType.TEXT_PLAIN_VALUE,
								"Article avec une figure.".getBytes(StandardCharsets.UTF_8)))
						.file(new MockMultipartFile("image", "premiere-page.png", MediaType.IMAGE_PNG_VALUE, firstPage))
						.header("Authorization", bearerToken()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long id = com.jayway.jsonpath.JsonPath.<Number>read(created, "$.id").longValue();
		String firstUrl = com.jayway.jsonpath.JsonPath.read(created, "$.image_url");

		String replaced = mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/admin/documents/" + id + "/image")
						.file(new MockMultipartFile("image", "figure.png", MediaType.IMAGE_PNG_VALUE, figure))
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		// Le navigateur garde l'image 7 jours : une nouvelle image doit avoir une nouvelle adresse.
		org.assertj.core.api.Assertions.assertThat((String) com.jayway.jsonpath.JsonPath.read(replaced, "$.image_url"))
				.isNotEqualTo(firstUrl);
		mockMvc.perform(get("/api/v1/documents/" + id + "/image").header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(content().bytes(figure));
	}

	@Test
	void adminSupervisesButDoesNotImportDocuments() throws Exception {
		mockMvc.perform(multipart("/api/v1/documents")
						.file(new MockMultipartFile("fichier", "admin.txt", MediaType.TEXT_PLAIN_VALUE,
								"Document importe par l'administrateur.".getBytes(StandardCharsets.UTF_8)))
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCannotValidateANotice() throws Exception {
		DocumentEntity document = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/documents/" + document.getId() + "/metadata")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titre":"Titre","date_publication":"2025-01-01","visibilite":"PUBLIC",
								 "auteurs":[{"nom_complet":"Sarah Lemaire"}],"mots_cles":[]}
								""")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCannotChangeItsOwnRole() throws Exception {
		UserEntity admin = userRepository.findByEmailIgnoreCase("admin@metamind.example").orElseThrow();

		mockMvc.perform(patch("/api/v1/admin/users/" + admin.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"LIBRARIAN\"}")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isConflict());
	}

	@Test
	void aPublicNoticeCanBeTranslatedAndIsKeptForTheNextReader() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.PUBLIE)
				.filter(item -> item.getVisibility() == DocumentVisibility.PUBLIC)
				.findFirst()
				.orElseThrow();

		// Pas encore traduite : la fiche recoit tout de suite la notice d'origine (202).
		// Une fois preparee, la traduction vient du cache (la requete par document et langue doit fonctionner).
		translationRepository.deleteAll();
		mockMvc.perform(get("/api/v1/publications/" + publication.getId() + "/traduction").param("langue", "nl"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.traduite", is(false)));
		translationService.translate(publication.getId(), "nl", null);
		mockMvc.perform(get("/api/v1/publications/" + publication.getId() + "/traduction").param("langue", "nl"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.langue", is("nl")));
	}

	@Test
	void anEnglishTitleIsTranslatedEvenWhenTheSummaryIsAlreadyFrench() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.PUBLIE)
				.filter(item -> item.getVisibility() == DocumentVisibility.PUBLIC)
				.findFirst()
				.orElseThrow();
		var metadata = metadataRepository.findByDocumentId(publication.getId()).orElseThrow();
		metadata.validate("Mapping and dynamics of woody cover in the thickets of the south: contributions from the district",
				"Les ecosystemes arides du district sont domines par les fourres et restent difficiles a cartographier dans cette region pour les chercheurs.",
				metadata.getPublicationDate(), metadata.getClassification(), null);
		metadataRepository.save(metadata);
		translationRepository.deleteAll();

		// Le resume est francais mais le titre anglais : en francais, la notice doit quand meme etre traduite.
		mockMvc.perform(get("/api/v1/publications/" + publication.getId() + "/traduction").param("langue", "fr"))
				.andExpect(status().isAccepted());
		translationService.translate(publication.getId(), "fr", null);
		mockMvc.perform(get("/api/v1/publications/" + publication.getId() + "/traduction").param("langue", "fr"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.langue_source", is("en")));
	}

	@Test
	void catalogueShowsTheTranslationPreparedForTheChosenLanguage() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.PUBLIE)
				.filter(item -> item.getVisibility() == DocumentVisibility.PUBLIC)
				.findFirst()
				.orElseThrow();
		translationRepository.save(new be.icc.metamind.publication.PublicationTranslationEntity(
				publication, "nl", "empreinte", "Vertaalde titel", "Vertaalde samenvatting", "[\"trefwoord\"]", "test"));

		mockMvc.perform(get("/api/v1/search").param("size", "100").param("affichage", "nl"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu[?(@.id == " + publication.getId() + ")].titre").value("Vertaalde titel"))
				.andExpect(jsonPath("$.contenu[?(@.id == " + publication.getId() + ")].resume").value("Vertaalde samenvatting"));
		// Sans langue d'affichage, la notice d'origine reste intacte.
		mockMvc.perform(get("/api/v1/search").param("size", "100"))
				.andExpect(jsonPath("$.contenu[?(@.id == " + publication.getId() + ")].titre").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Vertaalde titel"))));
	}

	@Test
	void adminSetsTheInstitutionPhotoShownOnTheHomePage() throws Exception {
		InstitutionEntity institution = institutionRepository.findByCodeIgnoreCase("INST-A").orElseThrow();
		byte[] photo = new byte[] { 7, 7, 7 };

		mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/institutions/" + institution.getId() + "/photo")
						.file(new MockMultipartFile("image", "campus.jpg", MediaType.IMAGE_JPEG_VALUE, photo))
						.param("credit", "Photo : Auteur, CC BY 4.0, Wikimedia Commons")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/institutions/photos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].credit", is("Photo : Auteur, CC BY 4.0, Wikimedia Commons")));
		mockMvc.perform(get("/api/v1/institutions/" + institution.getId() + "/photo"))
				.andExpect(status().isOk())
				.andExpect(content().bytes(photo));
	}

	@Test
	void institutionPhotoRequiresItsCredit() throws Exception {
		InstitutionEntity institution = institutionRepository.findByCodeIgnoreCase("INST-A").orElseThrow();
		mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/institutions/" + institution.getId() + "/photo")
						.file(new MockMultipartFile("image", "campus.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] { 1 }))
						.param("credit", " ")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void librarianCannotReplaceTheImageOfADocument() throws Exception {
		mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/admin/documents/1/image")
						.file(new MockMultipartFile("image", "figure.png", MediaType.IMAGE_PNG_VALUE, new byte[] { 1 }))
						.header("Authorization", bearerToken()))
				.andExpect(status().isForbidden());
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
	void passwordResetRequestDoesNotExposeAccountExistence() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password-reset/request")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"sarah@institution-a.example\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/password-reset/request")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"unknown@institution-a.example\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void onlyOneResetLinkIsValidAndRequestsAreSpacedOut() throws Exception {
		UserEntity sarah = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/v1/auth/password-reset/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"sarah@institution-a.example\"}"))
					.andExpect(status().isOk());
		}
		assertThat(passwordResetTokenRepository.findByUser_IdAndUsedAtIsNull(sarah.getId())).hasSize(1);
	}

	@Test
	void aPendingAccountGetsNoResetLink() throws Exception {
		InstitutionEntity institution = institutionRepository.findByCodeIgnoreCase("INST-A").orElseThrow();
		UserEntity pending = new UserEntity("Paul", "Martin", "paul@institution-a.example", passwordService.hash("55843500"), UserRole.LIBRARIAN, institution);
		pending.markPendingValidation();
		userRepository.save(pending);

		mockMvc.perform(post("/api/v1/auth/password-reset/request")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"paul@institution-a.example\"}"))
				.andExpect(status().isOk());
		assertThat(passwordResetTokenRepository.findByUser_IdAndUsedAtIsNull(pending.getId())).isEmpty();
	}

	@Test
	void passwordResetRejectsUnknownToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"token\":\"invalid\",\"password\":\"NouveauMot123\"}"))
				.andExpect(status().isBadRequest());
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
	void anUnknownInstitutionIsRequestedThenValidatedByTheAdministrator() throws Exception {
		String body = """
				{
				  "firstName": "Lea",
				  "lastName": "Dumont",
				  "email": "lea.dumont@nouvelle-ecole.test",
				  "institution": "nouvelle-ecole.test",
				  "password": "55843500"
				}
				""";
		// Domaine inconnu sans nom d'institution : l'interface doit demander ce nom.
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content(body.replace("\"password\"", "\"nom_institution\": \"Nouvelle Ecole\", \"password\"")))
				.andExpect(status().isCreated());
		InstitutionEntity requested = institutionRepository.findByEmailDomainIgnoreCase("nouvelle-ecole.test").orElseThrow();
		assertThat(requested.isPending()).isTrue();
		assertThat(requested.isActive()).isFalse();

		// Compte en attente : le message dit pourquoi la connexion est refusee.
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"lea.dumont@nouvelle-ecole.test\",\"password\":\"55843500\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/admin/institutions/" + requested.getId())
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"actif\": true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.en_attente", is(false)))
				.andExpect(jsonPath("$.solde_credits", is(20)));
		assertThat(userRepository.findByEmailIgnoreCase("lea.dumont@nouvelle-ecole.test").orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIF);
	}

	@Test
	void personalAddressesCannotRequestAnInstitution() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
						{"firstName":"Lea","lastName":"Dumont","email":"lea@gmail.com","nom_institution":"Ecole","password":"55843500"}
						"""))
				.andExpect(status().isBadRequest());
		// Une faute de frappe sur un fournisseur personnel est refusee de la meme facon.
		for (String typo : new String[] {"gmai.com", "gmial.com", "hotmial.com", "outlok.com"}) {
			mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
							"{\"firstName\":\"Lea\",\"lastName\":\"Dumont\",\"email\":\"lea@" + typo + "\",\"nom_institution\":\"Ecole\",\"password\":\"55843500\"}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message", containsString("personnelle")));
		}
	}

	@Test
	void anAccountStaysWithTheInstitutionOfItsEmailDomain() throws Exception {
		UserEntity sarah = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		InstitutionEntity other = institutionRepository.findByCodeIgnoreCase("INST-B").orElseThrow();
		mockMvc.perform(patch("/api/v1/admin/users/" + sarah.getId())
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"institution_id\": " + other.getId() + "}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aLibrarianChangesTheirPasswordWithTheCurrentOne() throws Exception {
		UserEntity sarah = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		String token = bearerToken("sarah@institution-a.example", "558435");
		mockMvc.perform(put("/api/v1/users/" + sarah.getId() + "/password").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"mot_de_passe_actuel\":\"mauvais\",\"nouveau_mot_de_passe\":\"nouveau-secret\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/v1/users/" + sarah.getId() + "/password").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"mot_de_passe_actuel\":\"558435\",\"nouveau_mot_de_passe\":\"nouveau-secret\"}"))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"sarah@institution-a.example\",\"password\":\"nouveau-secret\"}"))
				.andExpect(status().isOk());
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
	void freeCreditPackIsRejected() throws Exception {
		String body = """
				{
				  "pack_id": 1,
				  "cgv_acceptees": true,
				  "renonciation_retractation_acceptee": true
				}
				""";

		mockMvc.perform(post("/api/v1/credits")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound());
	}

	@Test
	void librarianBuysCreditsAndAdminDoesNot() throws Exception {
		String body = "{\"pack_id\":2,\"cgv_acceptees\":true,\"renonciation_retractation_acceptee\":true}";
		// Cahier des charges B9 : le bibliothecaire achete ; Stripe n'est pas configure en test.
		mockMvc.perform(post("/api/v1/credits")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isServiceUnavailable());
		mockMvc.perform(post("/api/v1/credits")
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());
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
	void administratorActivationGrantsWelcomeCreditsOnlyOnce() throws Exception {
		InstitutionEntity institution = institutionRepository.findByNameIgnoreCase("Institution A").orElseThrow();

		mockMvc.perform(patch("/api/v1/admin/institutions/" + institution.getId())
					.header("Authorization", adminBearerToken())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"actif\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.solde_credits", is(20)))
				.andExpect(jsonPath("$.credits_bienvenue_accordes", is(true)));

		mockMvc.perform(patch("/api/v1/admin/institutions/" + institution.getId())
					.header("Authorization", adminBearerToken())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"actif\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.solde_credits", is(20)));
	}

	@Test
	void suspendedInstitutionCannotStartCreditCheckout() throws Exception {
		InstitutionEntity institution = institutionRepository.findByNameIgnoreCase("Institution A").orElseThrow();

		mockMvc.perform(patch("/api/v1/admin/institutions/" + institution.getId())
					.header("Authorization", adminBearerToken())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"achatsSuspendus\":true}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/credits")
					.header("Authorization", bearerToken())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"pack_id\":2,\"cgv_acceptees\":true,\"renonciation_retractation_acceptee\":true}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void publicSearchReturnsOnlyPublishedPublicDocuments() throws Exception {
		mockMvc.perform(get("/api/v1/search")
						.param("q", "institution")
						.param("author", "Sarah")
						.param("date_debut", "2026-01-01")
						.param("date_fin", "2026-12-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu", hasSize(1)))
				.andExpect(jsonPath("$.page", is(0)))
				.andExpect(jsonPath("$.size", is(20)))
				.andExpect(jsonPath("$.total_elements", is(1)))
				.andExpect(jsonPath("$.contenu[0].titre", is("Analyse automatique des metadonnees pour les depots institutionnels")))
				.andExpect(jsonPath("$.contenu[0].visibilite", is("PUBLIC")));
	}

	@Test
	void publicSearchFiltersByPublicationDate() throws Exception {
		mockMvc.perform(get("/api/v1/search")
						.param("q", "institution")
						.param("date_fin", "2025-12-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu", hasSize(0)))
				.andExpect(jsonPath("$.total_elements", is(0)));
	}

	@Test
	void documentListCanBeFilteredAndSortedForAdministration() throws Exception {
		InstitutionEntity institution = institutionRepository.findByCodeIgnoreCase("INST-B").orElseThrow();
		new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository).create(
				"Catalogue des theses validees",
				"Jan Peeters",
				2025,
				DocumentStatus.PUBLIE,
				DocumentVisibility.PUBLIC,
				List.of("these", "catalogue"),
				institution,
				null
		);
		new TestDocumentFactory(documentRepository, metadataRepository, authorRepository, keywordRepository, documentAuthorRepository, documentKeywordRepository).create(
				"Archive scientifique en attente",
				"Jan Peeters",
				2026,
				DocumentStatus.A_VALIDER,
				DocumentVisibility.INSTITUTION,
				List.of("archive", "validation"),
				institution,
				null
		);

		mockMvc.perform(get("/api/v1/documents")
						.header("Authorization", adminBearerToken())
						.param("institution_id", institution.getId().toString())
						.param("statut", "PUBLIE")
						.param("date_debut", "2025-01-01")
						.param("date_fin", "2025-12-31")
						.param("sort", "titre")
						.param("direction", "desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu", hasSize(1)))
				.andExpect(jsonPath("$.total_elements", is(1)))
				.andExpect(jsonPath("$.contenu[0].titre", is("Catalogue des theses validees")))
				.andExpect(jsonPath("$.contenu[0].institution", is("Institution B")));
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
				.andExpect(jsonPath("$.taille_max_upload_mo", is("50")))
				.andExpect(jsonPath("$.tentatives_connexion_max", is("5")))
				// Seuls les parametres reellement appliques sont exposes.
				.andExpect(jsonPath("$.prix_credit_eur").doesNotExist());

		mockMvc.perform(patch("/api/v1/admin/config")
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"modele_llm\": \"gemini-2.5-flash-lite\", \"taille_max_upload_mo\": \"1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.modele_llm", is("gemini-2.5-flash-lite")))
				.andExpect(jsonPath("$.taille_max_upload_mo", is("1")));

		mockMvc.perform(get("/api/v1/admin/logs")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenu[0].action", is("MODIFICATION_CONFIGURATION")))
				.andExpect(jsonPath("$.contenu[0].details", containsString("taille_max_upload_mo : 50 → 1")))
				.andExpect(jsonPath("$.contenu[0].auteur", containsString("admin")));

		mockMvc.perform(get("/api/v1/admin/logs").param("q", "taille_max")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total_elements", is(1)));

		// La nouvelle limite s'applique aussitot a l'import.
		mockMvc.perform(multipart("/api/v1/documents")
						.file(new MockMultipartFile("fichier", "gros.txt", MediaType.TEXT_PLAIN_VALUE, new byte[2 * 1024 * 1024]))
						.header("Authorization", bearerToken()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void configurationRefusesAParameterWithoutEffect() throws Exception {
		mockMvc.perform(patch("/api/v1/admin/config")
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prix_credit_eur\": \"0.60\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/v1/admin/config")
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"modele_llm\": \"gpt-4\"}"))
				.andExpect(status().isBadRequest());
	}


	@Test
	void administratorCanExportDocumentsAsCsv() throws Exception {
		mockMvc.perform(get("/api/v1/admin/reports/documents.csv")
						.header("Authorization", adminBearerToken()))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/csv"))
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("id,titre,statut,visibilite,institution,date_publication,classification")
						.contains("Analyse automatique des metadonnees pour les depots institutionnels"));
	}

	@Test
	void creditsAndExtractionWorkflowIsSecuredAndConsumesOneCredit() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		DocumentEntity publication = documentRepository.findAll().getFirst();
		String authorization = bearerToken();
		user.getInstitution().addCredits(3);
		institutionRepository.save(user.getInstitution());

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
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].solde_apres", is(2)));
	}

	@Test
	void batchExtractionConsumesOneCreditPerSuccessfulDocument() throws Exception {
		UserEntity user = userRepository.findByEmailIgnoreCase("sarah@institution-a.example").orElseThrow();
		List<DocumentEntity> publications = documentRepository.findAll().stream()
				.filter(document -> document.getInstitution().getId().equals(user.getInstitution().getId()))
				.toList();
		String authorization = bearerToken();
		user.getInstitution().addCredits(3);
		institutionRepository.save(user.getInstitution());

		String body = """
				{
				  "document_ids": [%d, %d]
				}
				""".formatted(publications.get(0).getId(), publications.get(1).getId());

		mockMvc.perform(post("/api/v1/documents/extractions")
						.header("Authorization", authorization)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.total", is(2)))
				.andExpect(jsonPath("$.succes", is(2)))
				.andExpect(jsonPath("$.echecs", is(0)))
				.andExpect(jsonPath("$.resultats", hasSize(2)));

		assertThat(institutionRepository.findById(user.getInstitution().getId()).orElseThrow().getCreditBalance()).isEqualTo(1);
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
	void publicPublicationStaysReadableWithAnExpiredOrInvalidToken() throws Exception {
		DocumentEntity publicPublication = documentRepository.findAll().stream()
				.filter(publication -> publication.getStatus() == DocumentStatus.PUBLIE)
				.filter(publication -> publication.getVisibility() == DocumentVisibility.PUBLIC)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(get("/api/v1/publications/" + publicPublication.getId())
						.header("Authorization", "Bearer expired-token"))
				.andExpect(status().isOk());
	}

	@Test
	void publicationStatusCannotPublishWithoutValidation() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PUBLIE\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("en la validant")));

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", adminBearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"PUBLIE\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message", containsString("Seul un bibliothecaire")));
	}

	@Test
	void publishedNoticeCanBeWithdrawnByInstitutionLibrarian() throws Exception {
		DocumentEntity publication = documentRepository.findAll().stream()
				.filter(item -> item.getStatus() == DocumentStatus.A_VALIDER)
				.findFirst()
				.orElseThrow();

		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"A_VALIDER\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("Seule une notice publiee")));

		publication.updateStatus(DocumentStatus.PUBLIE);
		documentRepository.save(publication);
		mockMvc.perform(put("/api/v1/publications/" + publication.getId() + "/status")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"A_VALIDER\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statut", is("A_VALIDER")));
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
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message", is("Ce document appartient a une autre institution.")));
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
				.andExpect(jsonPath("$.texte_extrait", is("Rapport interne reserve a l institution")))
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

		mockMvc.perform(get("/api/v1/documents/" + publication.getId() + "/metadata/historique")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(7)))
				.andExpect(jsonPath("$[?(@.champ == 'titre')].ancienne_valeur", hasSize(1)))
				.andExpect(jsonPath("$[?(@.champ == 'titre')].nouvelle_valeur", hasSize(1)));
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
				// La repartition publique / reservee ne porte que sur les publications :
				// le document a valider, reserve a l'institution, n'y entre pas.
				.andExpect(jsonPath("$.publications_institution", is(0)))
				.andExpect(jsonPath("$.taux_validation", is(50.0)))
				.andExpect(jsonPath("$.taux_rejet", is(0.0)))
				.andExpect(jsonPath("$.distribution_classifications['Non renseigne']", is(2)));
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
				.andExpect(jsonPath("$.publications_institution", is(1)))
				.andExpect(jsonPath("$.distribution_types_documents['Non renseigne']", is(3)));
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

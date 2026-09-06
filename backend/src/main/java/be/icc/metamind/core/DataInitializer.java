package be.icc.metamind.core;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import be.icc.metamind.document.AuthorEntity;
import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorEntity;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentUploadService;
import be.icc.metamind.document.DocumentKeywordEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.KeywordEntity;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataStatus;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.PasswordService;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {
	private final InstitutionRepository institutionRepository;
	private final UserRepository userRepository;
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final AuthorRepository authorRepository;
	private final KeywordRepository keywordRepository;
	private final DocumentAuthorRepository documentAuthorRepository;
	private final DocumentKeywordRepository documentKeywordRepository;
	private final DocumentUploadService documentUploadService;
	private final PasswordService passwordService;
	private final boolean enabled;

	public DataInitializer(
			InstitutionRepository institutionRepository,
			UserRepository userRepository,
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			AuthorRepository authorRepository,
			KeywordRepository keywordRepository,
			DocumentAuthorRepository documentAuthorRepository,
			DocumentKeywordRepository documentKeywordRepository,
			DocumentUploadService documentUploadService,
			PasswordService passwordService,
			@Value("${metamind.seed-data:true}") boolean enabled
	) {
		this.institutionRepository = institutionRepository;
		this.userRepository = userRepository;
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.authorRepository = authorRepository;
		this.keywordRepository = keywordRepository;
		this.documentAuthorRepository = documentAuthorRepository;
		this.documentKeywordRepository = documentKeywordRepository;
		this.documentUploadService = documentUploadService;
		this.passwordService = passwordService;
		this.enabled = enabled;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled) {
			return;
		}

		InstitutionEntity institutionA = findOrCreateInstitution("INST-A", "Institution A", "institution-a.example");
		InstitutionEntity institutionB = findOrCreateInstitution("INST-B", "Institution B", "institution-b.example");
		InstitutionEntity platform = findOrCreateInstitution("META", "Metamind", "metamind.example");
		seedCredits(institutionA);
		seedCredits(institutionB);

		String password = passwordService.hash("558435");
		UserEntity sarah = createUserIfMissing("Sarah", "Lemaire", "sarah@institution-a.example", password, UserRole.LIBRARIAN, institutionA);
		UserEntity jan = createUserIfMissing("Jan", "Peeters", "jan@institution-b.example", password, UserRole.LIBRARIAN, institutionB);
		createUserIfMissing("Nadia", "Benali", "admin@metamind.example", password, UserRole.ADMIN, platform);
		createDocumentsIfMissing(institutionA, institutionB, sarah, jan);
	}

	private InstitutionEntity findOrCreateInstitution(String code, String name, String emailDomain) {
		return institutionRepository.findByCodeIgnoreCase(code)
				.orElseGet(() -> institutionRepository.save(new InstitutionEntity(code, name, emailDomain)));
	}

	private UserEntity createUserIfMissing(String firstName, String lastName, String email, String password, UserRole role, InstitutionEntity institution) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseGet(() -> userRepository.save(new UserEntity(firstName, lastName, email, password, role, institution)));
	}

	private void seedCredits(InstitutionEntity institution) {
		if (institution.getCreditBalance() == 0) {
			institution.addCredits(20);
		}
	}

	private void createDocumentsIfMissing(InstitutionEntity institutionA, InstitutionEntity institutionB, UserEntity sarah, UserEntity jan) {
		if (documentRepository.count() > 0) {
			return;
		}

		createDocument("Etude de la corrosion des aciers inoxydables en milieu marin", "Sarah Mertens", 2023, "Chimie",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("electrochimie", "corrosion", "materiaux"), institutionA, sarah);
		createDocument("Apprentissage profond pour la segmentation d'images medicales", "Yassine El Amrani", 2024, "Informatique",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("deep learning", "imagerie medicale", "reseaux de neurones"), institutionB, jan);
		createDocument("Impact du changement climatique sur les glaciers alpins", "Claire Dubois", 2022, "Geosciences",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("climatologie", "glaciologie", "teledetection"), institutionA, sarah);
		createDocument("Synthese de nanoparticules d'or pour la catalyse heterogene", "Thomas Lefevre", 2023, "Chimie",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("nanomateriaux", "catalyse", "or"), institutionB, jan);
		createDocument("Microbiote intestinal et maladies inflammatoires chroniques", "Nadia Benali", 2024, "Biologie",
				DocumentStatus.A_VALIDER, DocumentVisibility.INSTITUTION, List.of("microbiote", "immunologie", "sante"), institutionA, sarah);
		createDocument("Optimisation energetique des batiments passifs en Belgique", "Pieter Janssens", 2023, "Ingenierie",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("efficacite energetique", "batiment", "thermique"), institutionB, jan);
		createDocument("Detection de fraude par apprentissage automatique en milieu bancaire", "Amine Haddad", 2024, "Informatique",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("machine learning", "securite", "finance"), institutionA, sarah);
		createDocument("Role des ARN non codants dans la regulation de l'expression genique", "Emilie Rousseau", 2022, "Biologie moleculaire",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("genetique", "ARN", "expression genique"), institutionA, sarah);
		createDocument("Modelisation de la propagation des ondes sismiques", "Marc Vanden Bossche", 2023, "Geophysique",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("sismologie", "modelisation", "ondes"), institutionB, jan);
		createDocument("Traitement des eaux usees par procedes membranaires", "Fatima Zahra Idrissi", 2024, "Genie de l'environnement",
				DocumentStatus.A_VALIDER, DocumentVisibility.INSTITUTION, List.of("filtration", "eau", "membranes"), institutionB, jan);
		createDocument("Diagnostic assiste par intelligence artificielle en radiologie", "Lucas Moreau", 2024, "Medecine",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("intelligence artificielle", "radiologie", "diagnostic"), institutionA, sarah);
		createDocument("Batteries lithium-ion de nouvelle generation : etude electrochimique", "Sophie Laurent", 2023, "Chimie des materiaux",
				DocumentStatus.PUBLIE, DocumentVisibility.PUBLIC, List.of("batteries", "lithium", "stockage"), institutionB, jan);
	}

	private void createDocument(String title, String authorName, int year, String discipline, DocumentStatus status, DocumentVisibility visibility, List<String> keywords, InstitutionEntity institution, UserEntity importedBy) {
		String summary = "Publication de recherche en " + discipline.toLowerCase(Locale.ROOT) + " : " + title + ".";
		byte[] pdf = documentUploadService.buildTitlePagePdf(title, authorName, year, discipline, institution.getName());
		String fileName = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
		String filePath = documentUploadService.storeSeedDocumentPdf(pdf, fileName);
		String coverPath = documentUploadService.storePdfThumbnailFromBytes(pdf);
		DocumentEntity document = documentRepository.save(new DocumentEntity(
				fileName,
				filePath,
				(long) pdf.length,
				"application/pdf",
				title + "\n\n" + summary,
				status,
				visibility,
				institution,
				importedBy
		));
		document.updateCoverImagePath(coverPath);
		documentRepository.save(document);
		MetadataStatus metadataStatus = status == DocumentStatus.PUBLIE ? MetadataStatus.VALIDE : MetadataStatus.EN_ATTENTE;
		metadataRepository.save(new MetadataEntity(document, title, summary, LocalDate.of(year, 1, 1), discipline, metadataStatus));
		AuthorEntity author = authorRepository.findByFullNameIgnoreCase(authorName)
				.orElseGet(() -> authorRepository.save(new AuthorEntity(authorName, null)));
		documentAuthorRepository.save(new DocumentAuthorEntity(document, author, 1));
		for (String value : keywords) {
			KeywordEntity keyword = keywordRepository.findByLibelleIgnoreCase(value)
					.orElseGet(() -> keywordRepository.save(new KeywordEntity(value)));
			documentKeywordRepository.save(new DocumentKeywordEntity(document, keyword));
		}
	}
}

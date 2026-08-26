package be.icc.metamind.core;

import java.time.LocalDate;
import java.util.List;

import be.icc.metamind.document.AuthorEntity;
import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorEntity;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
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

		createDocument(
				"Analyse automatique des metadonnees pour les depots institutionnels",
				"Sarah Lemaire",
				2026,
				DocumentStatus.PUBLIE,
				DocumentVisibility.PUBLIC,
				List.of("Dublin Core", "metadonnees", "recherche"),
				institutionA,
				sarah
		);
		createDocument(
				"Validation humaine des suggestions produites par un modele de langage",
				"Jan Peeters",
				2025,
				DocumentStatus.A_VALIDER,
				DocumentVisibility.INSTITUTION,
				List.of("validation", "catalogage", "qualite"),
				institutionB,
				jan
		);
		createDocument(
				"Indexation multilingue de publications scientifiques",
				"Mina Laurent",
				2024,
				DocumentStatus.PUBLIE,
				DocumentVisibility.PUBLIC,
				List.of("indexation", "recherche", "multilingue"),
				institutionA,
				sarah
		);
	}

	private void createDocument(String title, String authorName, int year, DocumentStatus status, DocumentVisibility visibility, List<String> keywords, InstitutionEntity institution, UserEntity importedBy) {
		DocumentEntity document = documentRepository.save(new DocumentEntity(
				title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".txt",
				null,
				0L,
				"TXT",
				title,
				status,
				visibility,
				institution,
				importedBy
		));
		metadataRepository.save(new MetadataEntity(document, title, null, LocalDate.of(year, 1, 1), null, MetadataStatus.EN_ATTENTE));
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

package be.icc.metamind.core;


import be.icc.metamind.document.DocumentTypeEntity;
import be.icc.metamind.document.DocumentTypeRepository;
import be.icc.metamind.document.LanguageEntity;
import be.icc.metamind.document.LanguageRepository;
import be.icc.metamind.credit.CreditMovementEntity;
import be.icc.metamind.credit.CreditMovementRepository;
import be.icc.metamind.credit.CreditMovementType;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.PasswordService;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
	private final InstitutionRepository institutionRepository;
	private final UserRepository userRepository;
	private final PasswordService passwordService;
	private final boolean enabled;
	private final boolean resetSeed;
	private final String seedPassword;
	private final CreditMovementRepository creditMovementRepository;
	private final LanguageRepository languageRepository;
	private final DocumentTypeRepository documentTypeRepository;

	public DataInitializer(
			InstitutionRepository institutionRepository,
			UserRepository userRepository,
			PasswordService passwordService,
			CreditMovementRepository creditMovementRepository,
			LanguageRepository languageRepository,
			DocumentTypeRepository documentTypeRepository,
			@Value("${metamind.seed-data:false}") boolean enabled,
			@Value("${metamind.seed-reset:false}") boolean resetSeed,
			@Value("${metamind.seed-password:demo-password-change-me}") String seedPassword
	) {
		this.institutionRepository = institutionRepository;
		this.userRepository = userRepository;
		this.passwordService = passwordService;
		this.creditMovementRepository = creditMovementRepository;
		this.languageRepository = languageRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.enabled = enabled;
		this.resetSeed = resetSeed;
		this.seedPassword = seedPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled) {
			return;
		}
		seedReferenceData();

		InstitutionEntity institutionA = findOrCreateInstitution("INST-A", "Institution A", "institution-a.example");
		InstitutionEntity institutionB = findOrCreateInstitution("INST-B", "Institution B", "institution-b.example");
		InstitutionEntity platform = findOrCreateInstitution("META", "Metamind", "metamind.example");
		seedCredits(institutionA);
		seedCredits(institutionB);

		String password = passwordService.hash(seedPassword);
		createUserIfMissing("Sarah", "Lemaire", "sarah@institution-a.example", password, UserRole.LIBRARIAN, institutionA);
		createUserIfMissing("Jan", "Peeters", "jan@institution-b.example", password, UserRole.LIBRARIAN, institutionB);
		createUserIfMissing("Hatim", "Assal", "admin@metamind.example", password, UserRole.ADMIN, platform);
		if (resetSeed) {
			// Volontairement sans effet destructeur.
			// Cette option executait "TRUNCATE TABLE documents CASCADE" a chaque demarrage.
			// Posee par une variable d'environnement, elle effacait tout le corpus a chaque
			// deploiement, y compris des documents importes par de vrais utilisateurs.
			// Pour repartir d'une base propre en developpement : supprimer le volume Docker.
			log.warn("metamind.seed-reset est ignore : cette option effacait tous les documents. "
					+ "Supprimez le volume de la base pour repartir a zero.");
		}
	}

	private void seedReferenceData() {
		seedLanguage("fr", "Francais");
		seedLanguage("nl", "Nederlands");
		seedLanguage("en", "English");
		seedDocumentType("article", "Article scientifique");
		seedDocumentType("these", "These");
		seedDocumentType("memoire", "Memoire");
		seedDocumentType("rapport", "Rapport de recherche");
		seedDocumentType("chapitre", "Chapitre");
		seedDocumentType("communication", "Communication");
		seedDocumentType("preprint", "Prepublication");
		seedDocumentType("autre", "Autre");
	}

	private void seedLanguage(String code, String label) {
		languageRepository.findByCodeIgnoreCase(code)
				.orElseGet(() -> languageRepository.save(new LanguageEntity(code, label)));
	}

	private void seedDocumentType(String code, String label) {
		documentTypeRepository.findByCodeIgnoreCase(code)
				.orElseGet(() -> documentTypeRepository.save(new DocumentTypeEntity(code, label)));
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
		if (institution.getCreditBalance() > 0) {
			institution.markWelcomeCreditsGranted();
			return;
		}
		if (institution.grantWelcomeCredits(20)) {
			creditMovementRepository.save(new CreditMovementEntity(
					institution,
					CreditMovementType.OFFRE_BIENVENUE,
					20,
					institution.getCreditBalance(),
					"Offre de bienvenue de demonstration"
			));
		}
	}
}

package be.icc.metamind.core;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;
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
	private final PublicationRepository publicationRepository;
	private final PasswordService passwordService;
	private final boolean enabled;

	public DataInitializer(
			InstitutionRepository institutionRepository,
			UserRepository userRepository,
			PublicationRepository publicationRepository,
			PasswordService passwordService,
			@Value("${metamind.seed-data:true}") boolean enabled
	) {
		this.institutionRepository = institutionRepository;
		this.userRepository = userRepository;
		this.publicationRepository = publicationRepository;
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
		createUserIfMissing("Sarah", "Lemaire", "sarah@institution-a.example", password, UserRole.BIBLIOTHECAIRE, institutionA);
		createUserIfMissing("Jan", "Peeters", "jan@institution-b.example", password, UserRole.BIBLIOTHECAIRE, institutionB);
		createUserIfMissing("Nadia", "Benali", "admin@metamind.example", password, UserRole.ADMINISTRATEUR, platform);
		createPublicationsIfMissing(institutionA, institutionB);
	}

	private InstitutionEntity findOrCreateInstitution(String code, String name, String emailDomain) {
		return institutionRepository.findByCodeIgnoreCase(code)
				.orElseGet(() -> institutionRepository.save(new InstitutionEntity(code, name, emailDomain)));
	}

	private void createUserIfMissing(String firstName, String lastName, String email, String password, UserRole role, InstitutionEntity institution) {
		if (!userRepository.existsByEmailIgnoreCase(email)) {
			userRepository.save(new UserEntity(firstName, lastName, email, password, role, institution));
		}
	}

	private void seedCredits(InstitutionEntity institution) {
		if (institution.getCreditBalance() == 0) {
			institution.addCredits(20);
		}
	}

	private void createPublicationsIfMissing(InstitutionEntity institutionA, InstitutionEntity institutionB) {
		if (publicationRepository.count() > 0) {
			return;
		}

		publicationRepository.save(new PublicationEntity(
				"Analyse automatique des metadonnees pour les depots institutionnels",
				"Sarah Lemaire",
				2026,
				PublicationStatus.PUBLIE,
				Visibility.PUBLIC,
				"Dublin Core,metadonnees,recherche",
				institutionA
		));
		publicationRepository.save(new PublicationEntity(
				"Validation humaine des suggestions produites par un modele de langage",
				"Jan Peeters",
				2025,
				PublicationStatus.A_VALIDER,
				Visibility.INSTITUTION,
				"validation,catalogage,qualite",
				institutionB
		));
		publicationRepository.save(new PublicationEntity(
				"Indexation multilingue de publications scientifiques",
				"Mina Laurent",
				2024,
				PublicationStatus.PUBLIE,
				Visibility.PUBLIC,
				"indexation,recherche,multilingue",
				institutionA
		));
	}
}

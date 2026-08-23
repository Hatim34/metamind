package be.icc.metamind.core;

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
	private final PasswordService passwordService;
	private final boolean enabled;

	public DataInitializer(
			InstitutionRepository institutionRepository,
			UserRepository userRepository,
			PasswordService passwordService,
			@Value("${metamind.seed-data:true}") boolean enabled
	) {
		this.institutionRepository = institutionRepository;
		this.userRepository = userRepository;
		this.passwordService = passwordService;
		this.enabled = enabled;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled || institutionRepository.count() > 0 || userRepository.count() > 0) {
			return;
		}

		InstitutionEntity institutionA = institutionRepository.save(new InstitutionEntity("INST-A", "Institution A", "institution-a.example"));
		InstitutionEntity institutionB = institutionRepository.save(new InstitutionEntity("INST-B", "Institution B", "institution-b.example"));
		InstitutionEntity platform = institutionRepository.save(new InstitutionEntity("META", "Metamind", "metamind.example"));

		String password = passwordService.hash("MotDePasse123");
		userRepository.save(new UserEntity("Sarah", "Lemaire", "sarah@institution-a.example", password, UserRole.BIBLIOTHECAIRE, institutionA));
		userRepository.save(new UserEntity("Jan", "Peeters", "jan@institution-b.example", password, UserRole.BIBLIOTHECAIRE, institutionB));
		userRepository.save(new UserEntity("Nadia", "Benali", "admin@metamind.example", password, UserRole.ADMINISTRATEUR, platform));
	}
}

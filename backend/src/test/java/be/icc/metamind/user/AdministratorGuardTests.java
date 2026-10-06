package be.icc.metamind.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * La plateforme doit toujours conserver un administrateur actif.
 * Sans cette regle, un administrateur pouvait se supprimer, se retrograder ou se
 * desactiver et rendre l'application iningerable, sans retour possible par l'interface.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class AdministratorGuardTests {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InstitutionRepository institutionRepository;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private AdministratorGuard administratorGuard;

	@Test
	void refusesToDeleteTheLastActiveAdministrator() {
		UserEntity admin = saveAdmin("seul@metamind.example");

		assertThatThrownBy(() -> accountService.requestAccountDeletion(admin.getId()))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("dernier administrateur");
		assertThat(((ApiException) catchDeletion(admin.getId())).getStatus()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void allowsDeletionWhenAnotherActiveAdministratorRemains() {
		UserEntity premier = saveAdmin("premier@metamind.example");
		saveAdmin("second@metamind.example");

		assertThatCode(() -> accountService.requestAccountDeletion(premier.getId()))
				.doesNotThrowAnyException();
		assertThat(premier.getStatus()).isEqualTo(UserStatus.DESACTIVE);
	}

	@Test
	void refusesToDemoteOrDeactivateTheLastActiveAdministrator() {
		UserEntity admin = saveAdmin("seul@metamind.example");

		assertThatThrownBy(() -> administratorGuard.ensureAnotherActiveAdministratorRemains(admin, "modifier ce compte"))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void doesNotBlockOperationsOnLibrarians() {
		UserEntity librarian = saveUser("sarah@institution-a.example", UserRole.LIBRARIAN, UserStatus.ACTIF);

		assertThatCode(() -> administratorGuard.ensureAnotherActiveAdministratorRemains(librarian, "supprimer ce compte"))
				.doesNotThrowAnyException();
	}

	@Test
	void recognisesWhichChangesRemoveAdministration() {
		assertThat(administratorGuard.removesAdministration(UserRole.LIBRARIAN, null)).isTrue();
		assertThat(administratorGuard.removesAdministration(null, UserStatus.DESACTIVE)).isTrue();
		assertThat(administratorGuard.removesAdministration(UserRole.ADMIN, UserStatus.ACTIF)).isFalse();
		assertThat(administratorGuard.removesAdministration(null, null)).isFalse();
	}

	private Throwable catchDeletion(long id) {
		try {
			accountService.requestAccountDeletion(id);
			throw new AssertionError("la suppression aurait du etre refusee");
		}
		catch (ApiException exception) {
			return exception;
		}
	}

	private UserEntity saveAdmin(String email) {
		return saveUser(email, UserRole.ADMIN, UserStatus.ACTIF);
	}

	private UserEntity saveUser(String email, UserRole role, UserStatus status) {
		InstitutionEntity institution = institutionRepository.findByCodeIgnoreCase("GUARD")
				.orElseGet(() -> institutionRepository.save(
						new InstitutionEntity("GUARD", "Institution Guard", "guard.example")));
		UserEntity user = userRepository.save(new UserEntity(
				"Prenom", "Nom", email, passwordService.hash("mot-de-passe-long-2026"), role, institution));
		user.updateAdministration(role, status);
		return user;
	}
}

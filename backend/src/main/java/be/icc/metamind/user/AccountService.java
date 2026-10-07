package be.icc.metamind.user;

import java.util.Optional;
import java.util.Set;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.notification.AccountEvent;
import be.icc.metamind.auth.AuthResponse;
import be.icc.metamind.auth.JwtService;
import be.icc.metamind.auth.LoginAttemptService;
import be.icc.metamind.auth.LoginRequest;
import be.icc.metamind.auth.RegisterRequest;
import be.icc.metamind.auth.RegistrationResponse;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
	/** Fournisseurs d'adresses personnelles : ils ne designent aucune institution. */
	private static final Set<String> PERSONAL_EMAIL_DOMAINS = Set.of(
			"gmail.com", "googlemail.com", "outlook.com", "outlook.be", "hotmail.com", "hotmail.be", "hotmail.fr", "live.com", "live.be",
			"yahoo.com", "yahoo.fr", "icloud.com", "me.com", "proton.me", "protonmail.com", "gmx.com", "gmx.net", "skynet.be", "telenet.be", "proximus.be"
	);

	private final UserRepository userRepository;
	private final InstitutionRepository institutionRepository;
	private final PasswordService passwordService;
	private final JwtService jwtService;
	private final LoginAttemptService loginAttemptService;
	private final AdministratorGuard administratorGuard;
	private final ApplicationEventPublisher events;

	public AccountService(UserRepository userRepository, InstitutionRepository institutionRepository, PasswordService passwordService, JwtService jwtService, LoginAttemptService loginAttemptService, AdministratorGuard administratorGuard, ApplicationEventPublisher events) {
		this.events = events;
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.passwordService = passwordService;
		this.jwtService = jwtService;
		this.loginAttemptService = loginAttemptService;
		this.administratorGuard = administratorGuard;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		loginAttemptService.assertAllowed(request.email());
		UserEntity user;
		try {
			user = userRepository.findByEmailIgnoreCase(request.email())
					.filter(account -> passwordService.matches(request.password(), account.getPasswordHash()))
					.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Les identifiants sont incorrects."));
		} catch (ApiException exception) {
			if (exception.getStatus() == HttpStatus.UNAUTHORIZED) {
				loginAttemptService.recordFailure(request.email());
			}
			throw exception;
		}

		loginAttemptService.reset(request.email());
		// Mot de passe correct mais compte inutilisable : la personne doit savoir pourquoi.
		if (user.getStatus() == UserStatus.EN_ATTENTE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Votre compte attend la validation d'un administrateur.");
		}
		if (user.getStatus() != UserStatus.ACTIF) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Ce compte a ete desactive. Contactez l'administrateur de la plateforme.");
		}
		return new AuthResponse(jwtService.createToken(user), (int) jwtService.sessionSeconds(), UserResponse.from(user));
	}

	@Transactional
	public RegistrationResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "Un compte existe deja avec cet email.");
		}

		InstitutionEntity institution = institutionForRegistration(request, email);
		UserEntity user = new UserEntity(
				request.firstName(),
				request.lastName(),
				email,
				passwordService.hash(request.password()),
				UserRole.LIBRARIAN,
				institution
		);
		user.markPendingValidation();
		user.choosePreferredLanguage(request.language());

		UserEntity saved = userRepository.save(user);
		events.publishEvent(new AccountEvent.Requested(saved.getId(), institution.isPending() ? institution.getName() : null));
		return new RegistrationResponse(
				saved.getStatus().name(),
				institution.isPending()
						? "Votre demande a ete envoyee. L'administrateur doit valider l'institution et votre compte avant que vous puissiez vous connecter."
						: "Votre compte a ete cree. Il doit etre valide par un administrateur avant que vous puissiez vous connecter.",
				UserResponse.from(saved)
		);
	}

	@Transactional(readOnly = true)
	public UserEntity authenticate(String authorizationHeader) {
		UserEntity user = findUser(jwtService.readUserId(authorizationHeader));
		if (user.getStatus() != UserStatus.ACTIF) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le compte utilisateur est desactive.");
		}
		return user;
	}

	/**
	 * Les fiches et fichiers publics restent accessibles lorsqu'un navigateur conserve
	 * un JWT expire. Sans cela, un simple ancien jeton transforme une ressource publique
	 * en 401. Les ressources privees restent ensuite refusees par leur controle d'acces.
	 */
	@Transactional(readOnly = true)
	public UserEntity authenticateOptional(String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			return null;
		}
		try {
			return authenticate(authorizationHeader);
		} catch (ApiException exception) {
			if (exception.getStatus() == HttpStatus.UNAUTHORIZED) {
				return null;
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public UserEntity authenticateSelfOrAdmin(long id, String authorizationHeader) {
		UserEntity currentUser = authenticate(authorizationHeader);
		if (currentUser.getRole() == UserRole.ADMIN || currentUser.getId() == id) {
			return currentUser;
		}
		throw new ApiException(HttpStatus.FORBIDDEN, "Cette action n'est pas autorisee pour ce compte.");
	}

	@Transactional(readOnly = true)
	public UserEntity authenticateAdmin(String authorizationHeader) {
		UserEntity currentUser = authenticate(authorizationHeader);
		if (currentUser.getRole() == UserRole.ADMIN) {
			return currentUser;
		}
		throw new ApiException(HttpStatus.FORBIDDEN, "Cette action est reservee a l'administrateur.");
	}

	@Transactional(readOnly = true)
	public UserResponse getProfile(long id) {
		return UserResponse.from(findUser(id));
	}

	@Transactional(readOnly = true)
	public PersonalDataExportResponse exportPersonalData(long id) {
		return PersonalDataExportResponse.from(findUser(id));
	}

	@Transactional
	public UserResponse updateProfile(long id, UpdateProfileRequest request) {
		UserEntity user = findUser(id);
		InstitutionEntity institution = findInstitution(request.institution());
		validateActiveInstitution(institution);
		validateEmailDomain(user.getEmail(), institution);
		user.updateProfile(request.firstName(), request.lastName(), institution);
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse requestAccountDeletion(long id) {
		UserEntity user = findUser(id);
		administratorGuard.ensureAnotherActiveAdministratorRemains(user, "supprimer ce compte");
		user.anonymizeAndDeactivate();
		return UserResponse.from(user);
	}

	private UserEntity findUser(long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
	}

	/**
	 * L'institution vient du domaine de l'adresse email. Si aucune institution n'a ce domaine,
	 * la personne peut en demander l'ajout en donnant son nom : l'institution est creee
	 * inactive et l'administrateur decide.
	 */
	private InstitutionEntity institutionForRegistration(RegisterRequest request, String email) {
		String domain = email.substring(email.indexOf('@') + 1);
		InstitutionEntity institution = Optional.ofNullable(request.institution())
				.filter(value -> !value.isBlank())
				.flatMap(value -> institutionRepository.findByNameIgnoreCase(value.trim())
						.or(() -> institutionRepository.findByEmailDomainIgnoreCase(value.trim())))
				.or(() -> institutionRepository.findByEmailDomainIgnoreCase(domain))
				.orElse(null);
		if (institution != null) {
			if (!institution.isActive() && !institution.isPending()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "L'institution demandee est inactive.");
			}
			validateEmailDomain(email, institution);
			return institution;
		}
		String name = request.newInstitutionName() == null ? "" : request.newInstitutionName().trim();
		if (name.isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Aucune institution n'est inscrite pour le domaine " + domain + ".");
		}
		if (PERSONAL_EMAIL_DOMAINS.contains(domain)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Utilisez l'adresse email de votre institution, pas une adresse personnelle.");
		}
		if (institutionRepository.findByNameIgnoreCase(name).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "Une institution porte deja ce nom avec un autre domaine email. Contactez l'administrateur.");
		}
		return institutionRepository.save(InstitutionEntity.requested(name, domain));
	}

	@Transactional
	public void changePassword(long id, ChangePasswordRequest request) {
		UserEntity user = findUser(id);
		if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
		}
		user.changePassword(passwordService.hash(request.newPassword()));
	}

	private InstitutionEntity findInstitution(String value) {
		return institutionRepository.findByNameIgnoreCase(value)
				.or(() -> institutionRepository.findByEmailDomainIgnoreCase(value))
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution demandee est introuvable."));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private void validateActiveInstitution(InstitutionEntity institution) {
		if (!institution.isActive()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "L'institution demandee est inactive.");
		}
	}

	private void validateEmailDomain(String email, InstitutionEntity institution) {
		String expectedDomain = institution.getEmailDomain().trim().toLowerCase();
		if (!email.endsWith("@" + expectedDomain)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "L'email ne correspond pas au domaine de l'institution.");
		}
	}
}

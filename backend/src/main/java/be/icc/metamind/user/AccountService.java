package be.icc.metamind.user;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.auth.AuthResponse;
import be.icc.metamind.auth.JwtService;
import be.icc.metamind.auth.LoginAttemptService;
import be.icc.metamind.auth.LoginRequest;
import be.icc.metamind.auth.RegisterRequest;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.institution.InstitutionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
	private final UserRepository userRepository;
	private final InstitutionRepository institutionRepository;
	private final PasswordService passwordService;
	private final JwtService jwtService;
	private final LoginAttemptService loginAttemptService;

	public AccountService(UserRepository userRepository, InstitutionRepository institutionRepository, PasswordService passwordService, JwtService jwtService, LoginAttemptService loginAttemptService) {
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.passwordService = passwordService;
		this.jwtService = jwtService;
		this.loginAttemptService = loginAttemptService;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		loginAttemptService.assertAllowed(request.email());
		UserEntity user;
		try {
			user = userRepository.findByEmailIgnoreCase(request.email())
					.filter(account -> passwordService.matches(request.password(), account.getPasswordHash()))
					.filter(account -> account.getStatus() == UserStatus.ACTIF)
					.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Les identifiants sont incorrects."));
		} catch (ApiException exception) {
			if (exception.getStatus() == HttpStatus.UNAUTHORIZED) {
				loginAttemptService.recordFailure(request.email());
			}
			throw exception;
		}

		loginAttemptService.reset(request.email());
		return new AuthResponse(jwtService.createToken(user), UserResponse.from(user));
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "Un compte existe deja avec cet email.");
		}

		InstitutionEntity institution = findInstitution(request.institution());
		validateActiveInstitution(institution);
		validateEmailDomain(email, institution);
		UserEntity user = new UserEntity(
				request.firstName(),
				request.lastName(),
				email,
				passwordService.hash(request.password()),
				UserRole.LIBRARIAN,
				institution
		);

		UserEntity saved = userRepository.save(user);
		return new AuthResponse(jwtService.createToken(saved), UserResponse.from(saved));
	}

	@Transactional(readOnly = true)
	public UserEntity authenticate(String authorizationHeader) {
		UserEntity user = findUser(jwtService.readUserId(authorizationHeader));
		if (user.getStatus() != UserStatus.ACTIF) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le compte utilisateur est desactive.");
		}
		return user;
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
		user.anonymizeAndDeactivate();
		return UserResponse.from(user);
	}

	private UserEntity findUser(long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
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

package be.icc.metamind.user;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.auth.AuthResponse;
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

	public AccountService(UserRepository userRepository, InstitutionRepository institutionRepository, PasswordService passwordService) {
		this.userRepository = userRepository;
		this.institutionRepository = institutionRepository;
		this.passwordService = passwordService;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
				.filter(account -> passwordService.matches(request.password(), account.getPasswordHash()))
				.filter(account -> account.getStatus() == UserStatus.ACTIF)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Les identifiants sont incorrects."));

		return new AuthResponse("token-alpha-" + user.getId(), UserResponse.from(user));
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw new ApiException(HttpStatus.CONFLICT, "Un compte existe deja avec cet email.");
		}

		InstitutionEntity institution = findInstitution(request.institution());
		UserEntity user = new UserEntity(
				request.firstName(),
				request.lastName(),
				request.email().toLowerCase(),
				passwordService.hash(request.password()),
				UserRole.BIBLIOTHECAIRE,
				institution
		);

		UserEntity saved = userRepository.save(user);
		return new AuthResponse("token-alpha-" + saved.getId(), UserResponse.from(saved));
	}

	@Transactional(readOnly = true)
	public UserResponse getProfile(long id) {
		return UserResponse.from(findUser(id));
	}

	@Transactional
	public UserResponse updateProfile(long id, UpdateProfileRequest request) {
		UserEntity user = findUser(id);
		user.updateProfile(request.firstName(), request.lastName(), findInstitution(request.institution()));
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse requestAccountDeletion(long id) {
		UserEntity user = findUser(id);
		user.deactivate();
		return UserResponse.from(user);
	}

	private UserEntity findUser(long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Le compte utilisateur est introuvable."));
	}

	private InstitutionEntity findInstitution(String value) {
		return institutionRepository.findByCodeIgnoreCase(value)
				.or(() -> institutionRepository.findByNameIgnoreCase(value))
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "L'institution demandee est introuvable."));
	}
}

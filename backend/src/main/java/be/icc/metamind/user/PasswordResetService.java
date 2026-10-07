package be.icc.metamind.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.auth.PasswordResetConfirmRequest;
import be.icc.metamind.auth.PasswordResetRequest;
import be.icc.metamind.notification.AccountEvent;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PasswordResetService {
	/** Delai minimal entre deux liens pour le meme compte : evite d'inonder une boite mail. */
	private static final Duration MIN_INTERVAL = Duration.ofMinutes(2);

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordService passwordService;
	private final ApplicationEventPublisher events;
	private final long expirationMinutes;
	private final SecureRandom secureRandom = new SecureRandom();

	public PasswordResetService(UserRepository userRepository,
			PasswordResetTokenRepository tokenRepository,
			PasswordService passwordService,
			ApplicationEventPublisher events,
			@Value("${metamind.password-reset.expiration-minutes:30}") long expirationMinutes) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordService = passwordService;
		this.events = events;
		this.expirationMinutes = expirationMinutes;
	}

	/**
	 * La reponse est la meme que l'adresse existe ou non, et l'email part en arriere-plan :
	 * ni le message ni le temps de reponse ne revelent quelles adresses sont inscrites.
	 * Seul un compte actif recoit un lien ; un compte en attente ou desactive ne peut pas
	 * se connecter de toute facon.
	 */
	@Transactional
	public void requestReset(PasswordResetRequest request) {
		userRepository.findByEmailIgnoreCase(request.email().trim())
				.filter(user -> user.getStatus() == UserStatus.ACTIF)
				.ifPresent(user -> {
					Instant now = Instant.now();
					List<PasswordResetTokenEntity> pending = tokenRepository.findByUser_IdAndUsedAtIsNull(user.getId());
					boolean recent = pending.stream().anyMatch(token -> token.isUsable(now)
							&& token.getExpiresAt().minus(Duration.ofMinutes(expirationMinutes)).plus(MIN_INTERVAL).isAfter(now));
					if (recent) {
						return;
					}
					// Un seul lien valable a la fois : les precedents sont annules.
					pending.forEach(token -> token.markUsed(now));
					String token = createToken();
					tokenRepository.save(new PasswordResetTokenEntity(hash(token), user, now.plus(Duration.ofMinutes(expirationMinutes))));
					events.publishEvent(new AccountEvent.PasswordResetRequested(user.getId(), token, expirationMinutes));
				});
	}

	@Transactional
	public void confirmReset(PasswordResetConfirmRequest request) {
		PasswordResetTokenEntity resetToken = tokenRepository.findByTokenHash(hash(request.token()))
				.orElseThrow(() -> new ApiException(BAD_REQUEST, "Token de reinitialisation invalide ou expire."));
		Instant now = Instant.now();
		if (!resetToken.isUsable(now)) {
			throw new ApiException(BAD_REQUEST, "Token de reinitialisation invalide ou expire.");
		}
		resetToken.getUser().changePassword(passwordService.hash(request.password()));
		tokenRepository.findByUser_IdAndUsedAtIsNull(resetToken.getUser().getId()).forEach(token -> token.markUsed(now));
		resetToken.markUsed(now);
		tokenRepository.save(resetToken);
	}

	private String createToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 indisponible", exception);
		}
	}
}

package be.icc.metamind.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.auth.PasswordResetConfirmRequest;
import be.icc.metamind.auth.PasswordResetRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PasswordResetService {
	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordService passwordService;
	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final String publicUrl;
	private final long expirationMinutes;
	private final SecureRandom secureRandom = new SecureRandom();

	public PasswordResetService(UserRepository userRepository,
			PasswordResetTokenRepository tokenRepository,
			PasswordService passwordService,
			ObjectProvider<JavaMailSender> mailSenderProvider,
			@Value("${metamind.public-url:https://metamind-app.duckdns.org}") String publicUrl,
			@Value("${metamind.password-reset.expiration-minutes:30}") long expirationMinutes) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordService = passwordService;
		this.mailSenderProvider = mailSenderProvider;
		this.publicUrl = publicUrl;
		this.expirationMinutes = expirationMinutes;
	}

	@Transactional
	public void requestReset(PasswordResetRequest request) {
		userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
			String token = createToken();
			tokenRepository.save(new PasswordResetTokenEntity(hash(token), user,
					Instant.now().plus(Duration.ofMinutes(expirationMinutes))));
			sendOrLog(user.getEmail(), token);
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
		resetToken.markUsed(now);
		tokenRepository.save(resetToken);
	}

	private void sendOrLog(String email, String token) {
		String resetUrl = publicUrl + "/reset-password?token=" + token;
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (mailSender == null) {
			log.info("Lien de reinitialisation genere pour {} : {}", email, resetUrl);
			return;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(email);
			message.setSubject("Reinitialisation du mot de passe Metamind");
			message.setText("Utilisez ce lien avant son expiration : " + resetUrl);
			mailSender.send(message);
		}
		catch (RuntimeException exception) {
			// SMTP non configure ou indisponible : la demande ne doit pas echouer, on journalise le lien.
			log.warn("Envoi de l'email de reinitialisation impossible pour {}. Lien : {}", email, resetUrl);
		}
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

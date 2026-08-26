package be.icc.metamind.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import be.icc.metamind.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
	private static final int MAX_FAILURES = 5;
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

	private final Clock clock;
	private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

	public LoginAttemptService() {
		this(Clock.systemUTC());
	}

	LoginAttemptService(Clock clock) {
		this.clock = clock;
	}

	public void assertAllowed(String email) {
		String key = key(email);
		LoginAttempt attempt = attempts.get(key);
		if (attempt == null) {
			return;
		}

		Instant now = Instant.now(clock);
		if (attempt.blockedUntil() == null) {
			return;
		}
		if (attempt.blockedUntil().isAfter(now)) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives de connexion. Reessayez plus tard.");
		}
		attempts.remove(key);
	}

	public void recordFailure(String email) {
		String key = key(email);
		Instant now = Instant.now(clock);
		attempts.compute(key, (ignored, attempt) -> {
			boolean expired = attempt != null && attempt.blockedUntil() != null && !attempt.blockedUntil().isAfter(now);
			int failures = attempt == null || expired ? 1 : attempt.failures() + 1;
			Instant blockedUntil = failures >= MAX_FAILURES ? now.plus(BLOCK_DURATION) : null;
			return new LoginAttempt(failures, blockedUntil);
		});
	}

	public void reset(String email) {
		attempts.remove(key(email));
	}

	private String key(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private record LoginAttempt(int failures, Instant blockedUntil) {
	}
}

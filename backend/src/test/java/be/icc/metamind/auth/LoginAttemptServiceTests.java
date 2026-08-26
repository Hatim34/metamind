package be.icc.metamind.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import be.icc.metamind.api.ApiException;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTests {
	private final LoginAttemptService service = new LoginAttemptService(Clock.fixed(Instant.parse("2026-08-26T10:00:00Z"), ZoneOffset.UTC));

	@Test
	void blocksEmailAfterRepeatedFailures() {
		for (int attempt = 0; attempt < 5; attempt++) {
			service.recordFailure("sarah@institution-a.example");
		}

		assertThatThrownBy(() -> service.assertAllowed("sarah@institution-a.example"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("tentatives");
	}

	@Test
	void resetAllowsLoginAgain() {
		for (int attempt = 0; attempt < 5; attempt++) {
			service.recordFailure("sarah@institution-a.example");
		}

		service.reset("sarah@institution-a.example");

		service.assertAllowed("sarah@institution-a.example");
	}
}

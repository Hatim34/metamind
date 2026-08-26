package be.icc.metamind.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordServiceTests {
	private final PasswordService passwordService = new PasswordService();

	@Test
	void hashesPasswordWithoutKeepingPlainValue() {
		String hash = passwordService.hash("558435");

		assertThat(hash).isNotEqualTo("558435");
		assertThat(hash).isNotBlank();
		assertThat(hash).startsWith("$2");
	}

	@Test
	void usesDifferentSaltForEachHash() {
		String firstHash = passwordService.hash("558435");
		String secondHash = passwordService.hash("558435");

		assertThat(firstHash).isNotEqualTo(secondHash);
		assertThat(passwordService.matches("558435", firstHash)).isTrue();
		assertThat(passwordService.matches("558435", secondHash)).isTrue();
	}

	@Test
	void comparesPasswordWithHash() {
		String hash = passwordService.hash("558435");

		assertThat(passwordService.matches("558435", hash)).isTrue();
		assertThat(passwordService.matches("Erreur123", hash)).isFalse();
	}

	@Test
	void matchesDumpPasswordHash() {
		String dumpHash = "$2a$10$BzBBWEoS9SFGG59Lz6dfKOh7TK9h0uct3cz.DcbaUlGw2m5RaqVTG";

		assertThat(passwordService.matches("558435", dumpHash)).isTrue();
	}
}

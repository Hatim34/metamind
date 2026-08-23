package be.icc.metamind.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordServiceTests {
	private final PasswordService passwordService = new PasswordService();

	@Test
	void hashesPasswordWithoutKeepingPlainValue() {
		String hash = passwordService.hash("MotDePasse123");

		assertThat(hash).isNotEqualTo("MotDePasse123");
		assertThat(hash).isNotBlank();
	}

	@Test
	void comparesPasswordWithHash() {
		String hash = passwordService.hash("MotDePasse123");

		assertThat(passwordService.matches("MotDePasse123", hash)).isTrue();
		assertThat(passwordService.matches("Erreur123", hash)).isFalse();
	}
}

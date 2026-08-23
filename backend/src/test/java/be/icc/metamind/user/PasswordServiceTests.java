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
	}

	@Test
	void comparesPasswordWithHash() {
		String hash = passwordService.hash("558435");

		assertThat(passwordService.matches("558435", hash)).isTrue();
		assertThat(passwordService.matches("Erreur123", hash)).isFalse();
	}
}

package be.icc.metamind.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductionConfigurationValidatorTests {
	@Test
	void rejectsShortJwtSecret() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				"short", "jdbc:postgresql://db/metamind", "gemini", "gemini-key"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void rejectsGeminiWithoutApiKey() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				"12345678901234567890123456789012", "jdbc:postgresql://db/metamind", "gemini", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("GEMINI_API_KEY");
	}
}

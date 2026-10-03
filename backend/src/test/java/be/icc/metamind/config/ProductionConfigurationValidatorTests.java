package be.icc.metamind.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductionConfigurationValidatorTests {
	private static final String VALID_SECRET = "12345678901234567890123456789012";
	private static final String VALID_DATABASE = "jdbc:postgresql://db/metamind";

	@Test
	void rejectsShortJwtSecret() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				"short", VALID_DATABASE, "gemini", "gemini-key", "", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void rejectsInMemoryDatabase() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				VALID_SECRET, "jdbc:h2:mem:metamind", "gemini", "gemini-key", "", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("PostgreSQL");
	}

	@Test
	void rejectsGeminiWithoutApiKey() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				VALID_SECRET, VALID_DATABASE, "gemini", "", "", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("GEMINI_API_KEY");
	}

	@Test
	void rejectsTheLocalPlaceholderProviderInProduction() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				VALID_SECRET, VALID_DATABASE, "local", "", "", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("METAMIND_LLM_PROVIDER");
	}

	@Test
	void rejectsStripeWithoutWebhookSecret() {
		assertThatThrownBy(() -> new ProductionConfigurationValidator(
				VALID_SECRET, VALID_DATABASE, "gemini", "gemini-key", "sk_live_123", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("STRIPE_WEBHOOK_SECRET");
	}

	@Test
	void acceptsACompleteProductionConfiguration() {
		assertThatCode(() -> new ProductionConfigurationValidator(
				VALID_SECRET, VALID_DATABASE, "gemini", "gemini-key", "sk_live_123", "whsec_123"))
				.doesNotThrowAnyException();
	}
}

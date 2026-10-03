package be.icc.metamind.config;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionConfigurationValidator {
	public ProductionConfigurationValidator(
			@Value("${metamind.jwt.secret:}") String jwtSecret,
			@Value("${spring.datasource.url:}") String databaseUrl,
			@Value("${metamind.llm.provider:local}") String llmProvider,
			@Value("${metamind.gemini.api-key:}") String geminiApiKey,
			@Value("${metamind.stripe.secret-key:}") String stripeSecretKey,
			@Value("${metamind.stripe.webhook-secret:}") String stripeWebhookSecret
	) {
		if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("METAMIND_JWT_SECRET doit contenir au moins 32 octets en production.");
		}
		if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:h2:")) {
			throw new IllegalStateException("Une base PostgreSQL est obligatoire en production.");
		}
		if (llmProvider == null || llmProvider.isBlank() || "local".equalsIgnoreCase(llmProvider)) {
			throw new IllegalStateException(
					"METAMIND_LLM_PROVIDER ne peut pas valoir 'local' en production : "
							+ "l'extracteur local est un substitut de developpement et ne produit pas de vraies metadonnees."
			);
		}
		if ("gemini".equalsIgnoreCase(llmProvider) && (geminiApiKey == null || geminiApiKey.isBlank())) {
			throw new IllegalStateException("GEMINI_API_KEY est obligatoire lorsque Gemini est active.");
		}
		if (stripeSecretKey != null && !stripeSecretKey.isBlank()
				&& (stripeWebhookSecret == null || stripeWebhookSecret.isBlank())) {
			throw new IllegalStateException(
					"STRIPE_WEBHOOK_SECRET est obligatoire lorsque Stripe est active : "
							+ "sans signature verifiable, un appel au webhook pourrait crediter une institution."
			);
		}
	}
}

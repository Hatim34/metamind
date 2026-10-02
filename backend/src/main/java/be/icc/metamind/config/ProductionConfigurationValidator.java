package be.icc.metamind.config;

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
			@Value("${metamind.gemini.api-key:}") String geminiApiKey
	) {
		if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("METAMIND_JWT_SECRET doit contenir au moins 32 octets en production.");
		}
		if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:h2:")) {
			throw new IllegalStateException("Une base PostgreSQL est obligatoire en production.");
		}
		if ("gemini".equalsIgnoreCase(llmProvider) && (geminiApiKey == null || geminiApiKey.isBlank())) {
			throw new IllegalStateException("GEMINI_API_KEY est obligatoire lorsque Gemini est active.");
		}
	}
}

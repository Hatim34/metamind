package be.icc.metamind.publication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** En developpement sans LLM, la notice source reste simplement disponible. */
@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "local", matchIfMissing = true)
public class LocalPublicationTranslationProvider implements PublicationTranslationProvider {
	@Override
	public PublicationTranslation translate(TranslationSource source, String sourceLanguage, String targetLanguage) {
		return new PublicationTranslation(targetLanguage, sourceLanguage, source.title(), source.summary(), source.keywords(), false, source.classification());
	}
}

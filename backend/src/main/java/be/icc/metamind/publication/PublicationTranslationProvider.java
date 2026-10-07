package be.icc.metamind.publication;

/** Traduit les seules donnees descriptives d'une notice. */
public interface PublicationTranslationProvider {
	PublicationTranslation translate(TranslationSource source, String sourceLanguage, String targetLanguage);

	default String modelName() {
		return "local";
	}
}

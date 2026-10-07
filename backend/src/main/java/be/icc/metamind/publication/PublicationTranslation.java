package be.icc.metamind.publication;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Version localisee d'une notice, utilisee uniquement pour son affichage. */
public record PublicationTranslation(
		@JsonProperty("langue") String language,
		@JsonProperty("langue_source") String sourceLanguage,
		@JsonProperty("titre") String title,
		@JsonProperty("resume") String summary,
		@JsonProperty("mots_cles") List<String> keywords,
		@JsonProperty("traduite") boolean translated
) {
	public static PublicationTranslation source(PublicationResponse publication, String targetLanguage) {
		return new PublicationTranslation(
				targetLanguage,
				publication.language(),
				publication.title(),
				publication.summary(),
				publication.keywords(),
				false
		);
	}
}

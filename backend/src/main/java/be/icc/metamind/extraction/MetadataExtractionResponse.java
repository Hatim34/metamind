package be.icc.metamind.extraction;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataExtractionResponse(
		@JsonProperty("enrichissement_id")
		long enrichmentId,

		@JsonProperty("statut")
		String status,

		@JsonProperty("publication_id")
		long publicationId,

		@JsonProperty("titre")
		String title,

		@JsonProperty("titre_suggere")
		String suggestedTitle,

		@JsonProperty("auteur_suggere")
		String suggestedAuthor,

		@JsonProperty("mots_cles_suggeres")
		List<String> suggestedKeywords,

		@JsonProperty("solde_credits")
		int creditBalance
) {
}

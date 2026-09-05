package be.icc.metamind.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatisticsResponse(
		String scope,

		@JsonProperty("total_publications")
		long totalPublications,

		@JsonProperty("publications_publiees")
		long publishedPublications,

		@JsonProperty("publications_a_valider")
		long pendingValidationPublications,

		@JsonProperty("publications_publiques")
		long publicPublications,

		@JsonProperty("publications_institution")
		long institutionOnlyPublications,

		@JsonProperty("solde_credits")
		int creditBalance
) {
}

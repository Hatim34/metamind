package be.icc.metamind.statistics;

import java.util.Map;

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
		int creditBalance,

		@JsonProperty("taux_validation")
		double validationRate,

		@JsonProperty("taux_rejet")
		double rejectionRate,

		@JsonProperty("temps_moyen_traitement_heures")
		double averageProcessingHours,

		@JsonProperty("distribution_types_documents")
		Map<String, Long> documentTypeDistribution,

		@JsonProperty("distribution_classifications")
		Map<String, Long> classificationDistribution
) {
}

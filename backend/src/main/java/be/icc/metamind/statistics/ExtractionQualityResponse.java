package be.icc.metamind.statistics;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Indicateurs de fiabilite du LLM, calcules a partir des arbitrages du bibliothecaire.
 * Repond a la question de recherche : le modele extrait-il fiablement, et son score veut-il dire quelque chose ?
 */
public record ExtractionQualityResponse(
		String scope,

		@JsonProperty("suggestions_arbitrees")
		long arbitratedSuggestions,

		@JsonProperty("taux_acceptation_global")
		double overallAcceptanceRate,

		@JsonProperty("par_champ")
		List<FieldQuality> byField,

		@JsonProperty("calibration_score")
		List<CalibrationBucket> calibration
) {
	/** Qualite mesuree pour un champ donne (titre, auteurs, resume...). */
	public record FieldQuality(
			@JsonProperty("champ")
			String field,

			@JsonProperty("arbitrees")
			long arbitrated,

			@JsonProperty("acceptees")
			long accepted,

			@JsonProperty("modifiees")
			long modified,

			@JsonProperty("videes")
			long emptied,

			@JsonProperty("rejetees")
			long rejected,

			@JsonProperty("taux_acceptation")
			double acceptanceRate,

			@JsonProperty("distance_edition_moyenne")
			double averageEditDistance
	) {
	}

	/**
	 * Taux d'acceptation par tranche de score de confiance.
	 * Si le score est calibre, le taux d'acceptation doit croitre d'une tranche a la suivante.
	 */
	public record CalibrationBucket(
			@JsonProperty("tranche")
			String range,

			@JsonProperty("arbitrees")
			long arbitrated,

			@JsonProperty("acceptees")
			long accepted,

			@JsonProperty("taux_acceptation")
			double acceptanceRate
	) {
	}
}

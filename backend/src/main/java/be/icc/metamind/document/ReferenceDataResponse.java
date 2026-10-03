package be.icc.metamind.document;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Vocabulaires de reference utilises a la validation.
 * Le backend refuse tout code absent de ces listes : l'interface doit donc les lire ici
 * plutot que de les recopier, sous peine de proposer des valeurs rejetees.
 */
public record ReferenceDataResponse(
		@JsonProperty("langues")
		List<ReferenceValue> languages,

		@JsonProperty("types_documents")
		List<ReferenceValue> documentTypes
) {
	public record ReferenceValue(
			@JsonProperty("code")
			String code,

			@JsonProperty("libelle")
			String label
	) {
	}
}

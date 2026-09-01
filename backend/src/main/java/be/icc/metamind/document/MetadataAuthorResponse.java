package be.icc.metamind.document;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataAuthorResponse(
		@JsonProperty("nom_complet")
		String fullName,

		@JsonProperty("orcid")
		String orcid
) {
}

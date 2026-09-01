package be.icc.metamind.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetadataAuthorRequest(
		@JsonProperty("nom_complet")
		@NotBlank
		@Size(max = 255)
		String fullName,

		@JsonProperty("orcid")
		@Size(max = 30)
		String orcid
) {
}

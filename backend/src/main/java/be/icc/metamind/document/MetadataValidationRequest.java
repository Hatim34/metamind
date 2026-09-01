package be.icc.metamind.document;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MetadataValidationRequest(
		@JsonProperty("titre")
		@NotBlank
		@Size(max = 500)
		String title,

		@JsonProperty("resume")
		@Size(max = 5000)
		String summary,

		@JsonProperty("date_publication")
		LocalDate publicationDate,

		@JsonProperty("classification")
		@Size(max = 255)
		String classification,

		@JsonProperty("visibilite")
		@NotNull
		DocumentVisibility visibility,

		@JsonProperty("auteurs")
		@Size(max = 20)
		List<MetadataAuthorRequest> authors,

		@JsonProperty("mots_cles")
		@Size(max = 30)
		List<String> keywords
) {
}

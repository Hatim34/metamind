package be.icc.metamind.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rejet des metadonnees generees : le motif est obligatoire (exigence B6 du
 * cahier des charges).
 */
public record MetadataRejectionRequest(
		@JsonProperty("motif")
		@NotBlank
		@Size(max = 1000)
		String reason
) {
}

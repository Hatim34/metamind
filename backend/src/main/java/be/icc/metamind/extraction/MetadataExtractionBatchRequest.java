package be.icc.metamind.extraction;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataExtractionBatchRequest(
		@JsonProperty("document_ids")
		List<Long> documentIds
) {
}

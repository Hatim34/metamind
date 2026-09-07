package be.icc.metamind.extraction;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataExtractionBatchResponse(
		@JsonProperty("total")
		int total,

		@JsonProperty("succes")
		int successCount,

		@JsonProperty("echecs")
		int failureCount,

		@JsonProperty("resultats")
		List<MetadataExtractionBatchItemResponse> results
) {
	public static MetadataExtractionBatchResponse from(List<MetadataExtractionBatchItemResponse> results) {
		int successCount = (int) results.stream().filter(MetadataExtractionBatchItemResponse::success).count();
		return new MetadataExtractionBatchResponse(results.size(), successCount, results.size() - successCount, results);
	}
}

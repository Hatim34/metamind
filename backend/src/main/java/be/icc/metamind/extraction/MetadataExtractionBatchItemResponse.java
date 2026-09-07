package be.icc.metamind.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataExtractionBatchItemResponse(
		@JsonProperty("document_id")
		long documentId,

		@JsonProperty("statut")
		String status,

		@JsonProperty("succes")
		boolean success,

		@JsonProperty("message")
		String message,

		@JsonProperty("enrichissement_id")
		Long enrichmentId
) {
	public static MetadataExtractionBatchItemResponse success(MetadataExtractionResponse response) {
		return new MetadataExtractionBatchItemResponse(response.publicationId(), response.status(), true, "Extraction terminee.", response.enrichmentId());
	}

	public static MetadataExtractionBatchItemResponse failure(long documentId, String message) {
		return new MetadataExtractionBatchItemResponse(documentId, "ECHEC", false, message, null);
	}
}

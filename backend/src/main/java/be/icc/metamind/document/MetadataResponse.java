package be.icc.metamind.document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataResponse(
		long id,

		@JsonProperty("document_id")
		long documentId,

		@JsonProperty("titre")
		String title,

		@JsonProperty("resume")
		String summary,

		@JsonProperty("date_publication")
		LocalDate publicationDate,

		@JsonProperty("classification")
		String classification,

		@JsonProperty("visibilite")
		DocumentVisibility visibility,

		@JsonProperty("statut")
		MetadataStatus status,

		@JsonProperty("date_validation")
		LocalDateTime validatedAt,

		@JsonProperty("validee_par")
		Long validatedBy,

		@JsonProperty("auteurs")
		List<MetadataAuthorResponse> authors,

		@JsonProperty("mots_cles")
		List<String> keywords,

		@JsonProperty("langue")
		String language,

		@JsonProperty("type_document")
		String documentType,

		@JsonProperty("doi")
		String doi,

		@JsonProperty("texte_extrait")
		String extractedText,

		/** Confiance calculee par champ lors de la derniere extraction, entre 0 et 1. */
		@JsonProperty("confiances")
		Map<String, Double> confidences
) {
	public static MetadataResponse from(
			MetadataEntity metadata,
			List<MetadataAuthorResponse> authors,
			List<String> keywords,
			Map<String, Double> confidences
	) {
		return new MetadataResponse(
				metadata.getId(),
				metadata.getDocument().getId(),
				metadata.getTitre(),
				metadata.getResume(),
				metadata.getPublicationDate(),
				metadata.getClassification(),
				metadata.getDocument().getVisibility(),
				metadata.getStatus(),
				metadata.getValidatedAt(),
				metadata.getValidatedBy() == null ? null : metadata.getValidatedBy().getId(),
				authors,
				keywords,
				metadata.getLanguage() == null ? null : metadata.getLanguage().getCode(),
				metadata.getDocumentType() == null ? null : metadata.getDocumentType().getCode(),
				metadata.getDoi(),
				metadata.getDocument().getExtractedText(),
				confidences
		);
	}
}

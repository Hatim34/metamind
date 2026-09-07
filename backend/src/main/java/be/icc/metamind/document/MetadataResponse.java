package be.icc.metamind.document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

		@JsonProperty("texte_extrait")
		String extractedText
) {
	public static MetadataResponse from(MetadataEntity metadata, List<DocumentAuthorEntity> authors, List<DocumentKeywordEntity> keywords) {
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
				authors.stream()
						.map(author -> new MetadataAuthorResponse(author.getAuthor().getFullName(), author.getAuthor().getOrcid()))
						.toList(),
				keywords.stream()
						.map(keyword -> keyword.getKeyword().getLibelle())
						.toList(),
				metadata.getDocument().getExtractedText()
		);
	}
}

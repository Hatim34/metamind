package be.icc.metamind.publication;

import java.util.List;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.MetadataEntity;

public record PublicationResponse(
		long id,

		@JsonProperty("titre")
		String title,

		@JsonProperty("auteur")
		String author,

		String institution,

		@JsonProperty("annee")
		int year,

		@JsonProperty("resume")
		String summary,

		@JsonProperty("date_publication")
		LocalDate publicationDate,

		@JsonProperty("classification")
		String classification,

		@JsonProperty("langue")
		String language,

		@JsonProperty("type_document")
		String documentType,

		@JsonProperty("statut")
		PublicationStatus status,

		@JsonProperty("visibilite")
		Visibility visibility,

		@JsonProperty("mots_cles")
		List<String> keywords,

		@JsonProperty("image_url")
		String imageUrl,

		@JsonProperty("fichier_url")
		String fileUrl
) {
	public static PublicationResponse from(Publication publication) {
		return new PublicationResponse(
				publication.id(),
				publication.title(),
				publication.author(),
				publication.institution(),
				publication.year(),
				null,
				null,
				null,
				null,
				null,
				publication.status(),
				publication.visibility(),
				publication.keywords(),
				null,
				null
		);
	}

	public static PublicationResponse from(DocumentEntity document, MetadataEntity metadata, String author, List<String> keywords) {
		return new PublicationResponse(
				document.getId(),
				metadata == null || metadata.getTitre() == null ? document.getFileName() : metadata.getTitre(),
				author,
				document.getInstitution().getName(),
				metadata == null || metadata.getPublicationDate() == null ? 0 : metadata.getPublicationDate().getYear(),
				metadata == null ? null : metadata.getResume(),
				metadata == null ? null : metadata.getPublicationDate(),
				metadata == null ? null : metadata.getClassification(),
				metadata == null || metadata.getLanguage() == null ? null : metadata.getLanguage().getCode(),
				metadata == null || metadata.getDocumentType() == null ? null : metadata.getDocumentType().getLibelle(),
				toPublicationStatus(document.getStatus()),
				toVisibility(document.getVisibility()),
				keywords,
				document.getCoverImagePath() == null || document.getCoverImagePath().isBlank() ? null : "/api/v1/documents/" + document.getId() + "/image",
				document.getFilePath() == null || document.getFilePath().isBlank() ? null : "/api/v1/documents/" + document.getId() + "/file"
		);
	}

	private static PublicationStatus toPublicationStatus(DocumentStatus status) {
		return PublicationStatus.valueOf(status.name());
	}

	private static Visibility toVisibility(DocumentVisibility visibility) {
		return Visibility.valueOf(visibility.name());
	}

}

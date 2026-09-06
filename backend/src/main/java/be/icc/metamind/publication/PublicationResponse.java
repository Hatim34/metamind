package be.icc.metamind.publication;

import java.util.Arrays;
import java.util.List;

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

		@JsonProperty("statut")
		PublicationStatus status,

		@JsonProperty("visibilite")
		Visibility visibility,

		@JsonProperty("mots_cles")
		List<String> keywords,

		@JsonProperty("image_url")
		String imageUrl
) {
	public static PublicationResponse from(Publication publication) {
		return new PublicationResponse(
				publication.id(),
				publication.title(),
				publication.author(),
				publication.institution(),
				publication.year(),
				publication.status(),
				publication.visibility(),
				publication.keywords(),
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
				toPublicationStatus(document.getStatus()),
				toVisibility(document.getVisibility()),
				keywords,
				document.getCoverImagePath() == null || document.getCoverImagePath().isBlank() ? null : "/api/v1/documents/" + document.getId() + "/image"
		);
	}

	private static PublicationStatus toPublicationStatus(DocumentStatus status) {
		return PublicationStatus.valueOf(status.name());
	}

	private static Visibility toVisibility(DocumentVisibility visibility) {
		return Visibility.valueOf(visibility.name());
	}

	private static List<String> splitKeywords(String keywordsText) {
		return Arrays.stream(keywordsText.split(","))
				.map(String::trim)
				.filter(keyword -> !keyword.isBlank())
				.toList();
	}
}

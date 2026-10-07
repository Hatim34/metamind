package be.icc.metamind.publication;

import java.util.List;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentSummary;
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
		String fileUrl,

		@JsonProperty("texte_pret")
		boolean textReady
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
				null,
				false
		);
	}

	public static PublicationResponse from(DocumentEntity document, MetadataEntity metadata, String author, List<String> keywords) {
		return from(DocumentSummary.of(document), metadata, author, keywords);
	}

	public static PublicationResponse from(DocumentSummary document, MetadataEntity metadata, String author, List<String> keywords) {
		return new PublicationResponse(
				document.id(),
				metadata == null || metadata.getTitre() == null ? document.fileName() : metadata.getTitre(),
				author,
				document.institutionName(),
				metadata == null || metadata.getPublicationDate() == null ? 0 : metadata.getPublicationDate().getYear(),
				metadata == null ? null : metadata.getResume(),
				metadata == null ? null : metadata.getPublicationDate(),
				metadata == null ? null : metadata.getClassification(),
				metadata == null || metadata.getLanguage() == null ? null : metadata.getLanguage().getCode(),
				metadata == null || metadata.getDocumentType() == null ? null : metadata.getDocumentType().getLibelle(),
				toPublicationStatus(document.status()),
				toVisibility(document.visibility()),
				keywords,
				imageUrl(document),
				document.filePath() == null || document.filePath().isBlank() ? null : "/api/v1/documents/" + document.id() + "/file",
				document.textReady()
		);
	}

	/**
	 * L'image est gardee 7 jours par le navigateur : la version dans l'URL change quand
	 * l'image est remplacee (chaque image stockee a un chemin unique), sinon l'ancienne
	 * resterait affichee.
	 */
	private static String imageUrl(DocumentSummary document) {
		if (document.coverImagePath() == null || document.coverImagePath().isBlank()) {
			return null;
		}
		return "/api/v1/documents/" + document.id() + "/image?v=" + Integer.toHexString(document.coverImagePath().hashCode());
	}

	/** Meme notice, avec titre, resume et mots-cles affiches dans une autre langue. */
	public PublicationResponse withDisplayText(String translatedTitle, String translatedSummary, List<String> translatedKeywords, String translatedClassification) {
		return new PublicationResponse(id, translatedTitle == null ? title : translatedTitle, author, institution, year,
				translatedSummary == null ? summary : translatedSummary, publicationDate,
				translatedClassification == null ? classification : translatedClassification, language, documentType,
				status, visibility, translatedKeywords == null || translatedKeywords.isEmpty() ? keywords : translatedKeywords,
				imageUrl, fileUrl, textReady);
	}

	private static PublicationStatus toPublicationStatus(DocumentStatus status) {
		return PublicationStatus.valueOf(status.name());
	}

	private static Visibility toVisibility(DocumentVisibility visibility) {
		return Visibility.valueOf(visibility.name());
	}

}

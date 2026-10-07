package be.icc.metamind.document;

/**
 * Vue legere d'un document pour les listes : tout sauf le texte extrait.
 *
 * Le texte extrait pese environ 75 ko par document. Les listes (catalogue, file de
 * validation, statistiques) ne l'affichent jamais, mais charger l'entite complete le
 * ramenait quand meme : une quinzaine de megaoctets par affichage pour 183 documents.
 */
public record DocumentSummary(
		Long id,
		String fileName,
		String filePath,
		String coverImagePath,
		DocumentStatus status,
		DocumentVisibility visibility,
		Long institutionId,
		String institutionName,
		/** Texte du fichier lu : le document peut etre analyse par l'IA. */
		boolean textReady
) {
	public static DocumentSummary of(DocumentEntity document) {
		return new DocumentSummary(
				document.getId(),
				document.getFileName(),
				document.getFilePath(),
				document.getCoverImagePath(),
				document.getStatus(),
				document.getVisibility(),
				document.getInstitution().getId(),
				document.getInstitution().getName(),
				document.hasExtractedText()
		);
	}

	public boolean isPublished() {
		return status == DocumentStatus.PUBLIE && visibility == DocumentVisibility.PUBLIC;
	}
}

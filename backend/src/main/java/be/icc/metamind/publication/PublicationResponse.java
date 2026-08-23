package be.icc.metamind.publication;

import java.util.List;

public record PublicationResponse(
		long id,
		String title,
		String author,
		String institution,
		int year,
		PublicationStatus status,
		Visibility visibility,
		List<String> keywords
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
				publication.keywords()
		);
	}
}

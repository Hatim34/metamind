package be.icc.metamind.publication;

import java.util.Arrays;
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

	public static PublicationResponse from(PublicationEntity publication) {
		return new PublicationResponse(
				publication.getId(),
				publication.getTitle(),
				publication.getAuthor(),
				publication.getInstitution().getName(),
				publication.getYear(),
				publication.getStatus(),
				publication.getVisibility(),
				splitKeywords(publication.getKeywordsText())
		);
	}

	private static List<String> splitKeywords(String keywordsText) {
		return Arrays.stream(keywordsText.split(","))
				.map(String::trim)
				.filter(keyword -> !keyword.isBlank())
				.toList();
	}
}

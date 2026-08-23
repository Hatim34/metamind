package be.icc.metamind.publication;

import java.util.List;

public record PublicationRequest(
		String title,
		String author,
		String institution,
		int year,
		Visibility visibility,
		List<String> keywords
) {
}

package be.icc.metamind.publication;

import java.util.List;
import java.util.Locale;

public record Publication(
		long id,
		String title,
		String author,
		String institution,
		int year,
		PublicationStatus status,
		Visibility visibility,
		List<String> keywords
) {
	public boolean matches(String value) {
		return String.join(" ", title, author, institution, status.name(), visibility.name(), String.join(" ", keywords))
				.toLowerCase(Locale.ROOT)
				.contains(value);
	}
}

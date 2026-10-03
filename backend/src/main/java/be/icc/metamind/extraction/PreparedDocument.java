package be.icc.metamind.extraction;

import java.util.List;
import java.util.Set;

public record PreparedDocument(
		String normalizedText,
		String excerpt,
		List<Segment> segments,
		String language,
		String documentType,
		Set<String> dois,
		Set<String> orcids
) {
	public record Segment(String id, String text) {
	}

	public String segmentedExcerpt() {
		return segments.stream()
				.map(segment -> "[" + segment.id() + "] " + segment.text())
				.reduce((left, right) -> left + "\n\n" + right)
				.orElse("");
	}
}

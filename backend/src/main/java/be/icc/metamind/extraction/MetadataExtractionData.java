package be.icc.metamind.extraction;

import java.util.List;

public record MetadataExtractionData(
		String title,
		String author,
		List<String> keywords
) {
}

package be.icc.metamind.extraction;

import java.util.List;

public record MetadataExtractionResponse(
		long publicationId,
		String title,
		String suggestedTitle,
		String suggestedAuthor,
		List<String> suggestedKeywords,
		int creditBalance
) {
}

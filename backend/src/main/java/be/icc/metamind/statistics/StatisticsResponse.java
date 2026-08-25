package be.icc.metamind.statistics;

public record StatisticsResponse(
		String scope,
		long totalPublications,
		long publishedPublications,
		long pendingValidationPublications,
		long publicPublications,
		long institutionOnlyPublications,
		int creditBalance
) {
}

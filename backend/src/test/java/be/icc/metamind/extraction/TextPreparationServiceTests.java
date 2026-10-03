package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextPreparationServiceTests {
	private final TextPreparationService service = new TextPreparationService(15_000);

	@Test
	void normalizesTextRemovesRepeatedHeadersAndFindsIdentifiers() {
		PreparedDocument prepared = service.prepare("""
				Journal of Testing
				A meta-\ndata study

				DOI: 10.1234/TEST.42. ORCID 0000-0002-1825-0097
				Journal of Testing
				Conclusion.
				Journal of Testing
				""");

		assertThat(prepared.normalizedText()).contains("A metadata study").doesNotContain("Journal of Testing");
		assertThat(prepared.dois()).containsExactly("10.1234/TEST.42");
		assertThat(prepared.orcids()).containsExactly("0000-0002-1825-0097");
		assertThat(prepared.segments()).extracting(PreparedDocument.Segment::id).containsExactly("S1", "S2");
	}

	@Test
	void boundsTheExcerptWhileKeepingTheStartAndEnd() {
		String text = "START " + "a".repeat(2_000) + " END";

		PreparedDocument prepared = service.prepare(text, 1_000);

		assertThat(prepared.excerpt()).hasSizeLessThanOrEqualTo(1_000);
		assertThat(prepared.excerpt()).startsWith("START").endsWith("END");
		assertThat(prepared.segmentedExcerpt()).contains("[S1]");
	}
}

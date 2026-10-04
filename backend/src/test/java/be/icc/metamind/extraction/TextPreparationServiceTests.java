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
	void deducesTheDocumentTypeFromItsOwnWording() {
		assertThat(service.detectDocumentType("These de doctorat presentee par Sarah Lemaire")).isEqualTo("these");
		assertThat(service.detectDocumentType("Memoire de master en sciences informatiques")).isEqualTo("memoire");
		assertThat(service.detectDocumentType("Rapport technique numero 12")).isEqualTo("rapport");
		assertThat(service.detectDocumentType("Proceedings of the 4th conference on metadata")).isEqualTo("communication");
		assertThat(service.detectDocumentType("Journal of Information Science, received: 2026")).isEqualTo("article");
	}

	@Test
	void doesNotInventADocumentTypeWhenNoClueIsPresent() {
		assertThat(service.detectDocumentType("Analyse automatique des metadonnees")).isNull();
		assertThat(service.detectDocumentType("")).isNull();
		assertThat(service.detectDocumentType(null)).isNull();
	}

	@Test
	void prefersTheMostSpecificClueWhenSeveralMatch() {
		// Une these citant une revue reste une these.
		assertThat(service.detectDocumentType("These de doctorat publiee dans la revue Journal of Testing"))
				.isEqualTo("these");
	}

	@Test
	void keepsTheHeaderDoiAndIgnoresThoseCitedInTheBibliography() {
		String article = """
				Journal of Testing, vol. 4
				DOI: 10.1234/propre.2026

				Resume du travail presente ici.

				References
				Dupont, A. (2020). Autre travail. https://doi.org/10.9999/cite-par-erreur
				Martin, B. (2021). Encore un autre. 10.8888/aussi-cite
				""";

		PreparedDocument prepared = service.prepare(article);

		assertThat(prepared.documentDoi()).isEqualTo("10.1234/propre.2026");
		// Les DOI cites restent disponibles pour le recoupement, mais ne sont pas celui du document.
		assertThat(prepared.dois()).contains("10.9999/cite-par-erreur", "10.8888/aussi-cite");
	}

	@Test
	void reportsNoDoiWhenTheHeaderDoesNotCarryOne() {
		String sansDoi = "Titre sans identifiant.\n\nCorps du document.";

		assertThat(service.prepare(sansDoi).documentDoi()).isNull();
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

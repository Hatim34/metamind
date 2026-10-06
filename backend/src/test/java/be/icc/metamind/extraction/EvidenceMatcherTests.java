package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvidenceMatcherTests {
	private static final String DOCUMENT = """
			Het vergrote vaderland – en andere breukmomenten
			in het gebruik van het Nederlands
			Michel De Dobbeleer, Anne Peeters
			Samenvatting
			Dit artikel onderzoekt hoe de stripreeks Suske en Wiske de geschiedenis
			van Vlaanderen verbeeldt, van de eerste albums tot vandaag, en welke
			rol Willy Vandersteen daarbij speelde in de naoorlogse periode.
			""";

	@Test
	void findsATitleCutByALineBreak() {
		assertThat(EvidenceMatcher.find("titre",
				"Het vergrote vaderland - en andere breukmomenten in het gebruik van het Nederlands", DOCUMENT))
				.isNotNull();
	}

	@Test
	void findsEachAuthorOfAListSeparately() {
		assertThat(EvidenceMatcher.find("auteurs", "Michel De Dobbeleer, Anne Peeters", DOCUMENT)).isNotNull();
		// Un auteur absent du document suffit a refuser la preuve de toute la liste.
		assertThat(EvidenceMatcher.find("auteurs", "Michel De Dobbeleer, Jan Janssens", DOCUMENT)).isNull();
	}

	@Test
	void acceptsANearlyExactCopyOfALongSummary() {
		String summary = "Dit artikel onderzoekt hoe de stripreeks Suske en Wiske de geschiedenis van Vlaanderen verbeeldt, "
				+ "van de eerste albums tot vandaag, en welke rol Willy Vandersteen daarbij speelde in de naoorlogse tijd.";

		assertThat(EvidenceMatcher.find("resume", summary, DOCUMENT)).isNotNull();
	}

	@Test
	void findsTheYearOfAPublicationDate() {
		String document = "Vlaams Diergeneeskundig Tijdschrift, 2025, 94\nGepubliceerd op 27 oktober 2025";

		assertThat(EvidenceMatcher.find("date_publication", "2025-10-27", document)).startsWith("2025");
		assertThat(EvidenceMatcher.find("date_publication", "2019-01-01", document)).isNull();
	}

	@Test
	void refusesASummaryWrittenByTheModel() {
		String invented = "Cette etude explore la representation de l'histoire flamande dans une bande dessinee populaire, "
				+ "en analysant plusieurs albums parus depuis la guerre et le role de leur auteur principal.";

		assertThat(EvidenceMatcher.find("resume", invented, DOCUMENT)).isNull();
	}
}

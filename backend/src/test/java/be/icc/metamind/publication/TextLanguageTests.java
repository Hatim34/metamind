package be.icc.metamind.publication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextLanguageTests {
	@Test
	void recognisesTheLanguageOfTheDisplayedText() {
		assertThat(TextLanguage.guess("Le rôle des caprins dans le recyclage de l'azote et la fertilité des sols")).isEqualTo("fr");
		assertThat(TextLanguage.guess("A Challenging Age: Literature, the Climate Crisis and the Dialogue between Generations")).isEqualTo("en");
		assertThat(TextLanguage.guess("Levensverhalen van kinderloze 60-plussers: reflecties op kinderloosheid en eenzaamheid bij het ouder worden")).isEqualTo("nl");
	}

	@Test
	void staysSilentWhenTheTextIsTooShortToDecide() {
		assertThat(TextLanguage.guess("Thinking like a slag heap")).isNull();
		assertThat(TextLanguage.guess(null)).isNull();
	}
}

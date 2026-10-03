package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfidenceScorerTests {
	private final TextPreparationService preparationService = new TextPreparationService(15_000);
	private final ConfidenceScorer scorer = new ConfidenceScorer();

	@Test
	void capsTheScoreWhenEvidenceIsInvented() {
		PreparedDocument document = preparationService.prepare("Titre reel de la publication.");

		ConfidenceScore score = scorer.score("titre", "Titre reel de la publication", "citation inventee", 1, document);

		assertThat(score.value()).isLessThanOrEqualTo(0.30);
		assertThat(score.level()).isEqualTo("ROUGE");
	}

	@Test
	void penalizesADoiDifferentFromTheDetectedOne() {
		PreparedDocument document = preparationService.prepare("DOI 10.1234/reel.2026");

		ConfidenceScore matching = scorer.score("doi", "10.1234/reel.2026", "10.1234/reel.2026", 0.9, document);
		ConfidenceScore mismatching = scorer.score("doi", "10.9999/invente", "DOI", 0.9, document);

		assertThat(mismatching.value()).isLessThan(matching.value());
		assertThat(mismatching.signals()).contains("doi invalide ou different du doi detecte");
	}
}

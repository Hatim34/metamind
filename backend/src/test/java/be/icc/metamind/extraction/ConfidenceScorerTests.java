package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfidenceScorerTests {
	private final TextPreparationService preparationService = new TextPreparationService(15_000);
	private final ConfidenceScorer scorer = new ConfidenceScorer();

	@Test
	void capsTheScoreWhenEvidenceIsInvented() {
		PreparedDocument document = preparationService.prepare("Titre reel de la publication.");

		ConfidenceScore score = scorer.score("titre", "Titre reel de la publication", "citation inventee", 1.0, document);

		assertThat(score.value()).isLessThanOrEqualTo(0.30);
		assertThat(score.level()).isEqualTo("ROUGE");
	}

	@Test
	void doesNotInventAModelConfidenceWhenTheProviderGivesNone() {
		PreparedDocument document = preparationService.prepare("Titre reel de la publication.");

		ConfidenceScore score = scorer.score(
				"titre", "Titre reel de la publication", "Titre reel de la publication", null, document);

		assertThat(score.signals()).contains("confiance du modele non fournie : score fonde sur les verifications locales");
		assertThat(score.value()).isGreaterThan(0.30);
	}

	@Test
	void takesTheAnnouncedModelConfidenceIntoAccountWhenItIsProvided() {
		PreparedDocument document = preparationService.prepare("Titre reel de la publication.");

		ConfidenceScore confident = scorer.score(
				"titre", "Titre reel de la publication", "Titre reel de la publication", 1.0, document);
		ConfidenceScore doubtful = scorer.score(
				"titre", "Titre reel de la publication", "Titre reel de la publication", 0.0, document);

		assertThat(confident.value()).isGreaterThan(doubtful.value());
		assertThat(confident.signals()).contains("confiance annoncee par le modele : 100%");
		assertThat(doubtful.signals()).contains("confiance annoncee par le modele : 0%");
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

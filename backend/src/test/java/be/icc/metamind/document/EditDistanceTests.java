package be.icc.metamind.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EditDistanceTests {
	@Test
	void identicalValuesHaveNoDistance() {
		assertThat(EditDistance.normalized("Dublin Core", "dublin   core")).isZero();
	}

	@Test
	void twoEmptyValuesHaveNoDistance() {
		assertThat(EditDistance.normalized(null, "   ")).isZero();
	}

	@Test
	void aSmallCorrectionGivesASmallDistance() {
		double distance = EditDistance.normalized(
				"Analyse automatique des metadonnees",
				"Analyse automatique de metadonnees");

		assertThat(distance).isGreaterThan(0.0).isLessThan(0.1);
	}

	@Test
	void aCompleteRewriteGivesALargeDistance() {
		double distance = EditDistance.normalized("Dublin Core", "Catalogage avance des theses");

		assertThat(distance).isGreaterThan(0.5).isLessThanOrEqualTo(1.0);
	}

	@Test
	void erasingAValueGivesTheMaximumDistance() {
		assertThat(EditDistance.normalized("Dublin Core", "")).isEqualTo(1.0);
	}
}

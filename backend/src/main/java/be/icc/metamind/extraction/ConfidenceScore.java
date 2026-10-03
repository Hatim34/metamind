package be.icc.metamind.extraction;

import java.util.List;

public record ConfidenceScore(double value, List<String> signals) {
	public String level() {
		if (value >= 0.85) {
			return "VERT";
		}
		if (value >= 0.60) {
			return "ORANGE";
		}
		return "ROUGE";
	}
}

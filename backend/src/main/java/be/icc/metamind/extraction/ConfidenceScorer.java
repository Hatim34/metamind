package be.icc.metamind.extraction;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ConfidenceScorer {
	private static final Pattern DOI_PATTERN = Pattern.compile("(?i)^10\\.\\d{4,9}/[-._;()/:a-z0-9]+$");
	private static final Set<String> DOCUMENT_TYPES = Set.of(
			"article", "these", "memoire", "rapport", "chapitre", "communication", "preprint", "autre"
	);

	/**
	 * @param modelConfidence confiance annoncee par le modele, ou null s'il n'en a pas fourni.
	 *                        Dans ce cas le score repose uniquement sur les verifications locales :
	 *                        aucune valeur de remplacement n'est inventee.
	 */
	public ConfidenceScore score(
			String field,
			String value,
			String evidence,
			Double modelConfidence,
			PreparedDocument document
	) {
		List<String> signals = new ArrayList<>();
		if (value == null || value.isBlank()) {
			return new ConfidenceScore(0, List.of("valeur absente"));
		}
		boolean evidenceFound = evidence != null && !evidence.isBlank()
				&& normalized(document.normalizedText()).contains(normalized(evidence));
		if (evidenceFound) {
			signals.add("preuve retrouvee dans le document");
		} else {
			signals.add("preuve absente ou introuvable");
		}

		double heuristic = evidenceFound ? 0.70 : 0.20;
		heuristic = switch (field) {
			case "titre" -> titleScore(value, document, heuristic, signals);
			case "auteurs" -> authorsScore(value, document, heuristic, signals);
			case "doi" -> doiScore(value, document, heuristic, signals);
			case "date_publication" -> dateScore(value, heuristic, signals);
			case "langue" -> languageScore(value, document, heuristic, signals);
			case "type_document" -> vocabularyScore(value, DOCUMENT_TYPES, heuristic, signals);
			default -> heuristic;
		};
		double score;
		if (modelConfidence == null) {
			signals.add("confiance du modele non fournie : score fonde sur les verifications locales");
			score = clamp(heuristic);
		} else {
			signals.add("confiance annoncee par le modele : " + Math.round(clamp(modelConfidence) * 100) + "%");
			score = clamp((0.4 * clamp(modelConfidence)) + (0.6 * heuristic));
		}
		if (!evidenceFound) {
			score = Math.min(score, 0.30);
		}
		return new ConfidenceScore(score, List.copyOf(signals));
	}

	private double titleScore(String value, PreparedDocument document, double score, List<String> signals) {
		boolean validLength = value.length() >= 5 && value.length() <= 300;
		boolean inOpening = normalized(document.excerpt()).contains(normalized(value));
		if (validLength && inOpening) {
			signals.add("titre present dans l extrait initial");
			return Math.min(1, score + 0.25);
		}
		signals.add("titre absent de l extrait initial ou longueur invalide");
		return Math.max(0, score - 0.25);
	}

	private double authorsScore(String value, PreparedDocument document, double score, List<String> signals) {
		boolean allPresent = List.of(value.split("[,;]"))
				.stream().map(String::trim).filter(name -> !name.isBlank())
				.allMatch(name -> normalized(document.normalizedText()).contains(normalized(name)));
		if (allPresent) {
			signals.add("auteurs retrouves dans le document");
			return Math.min(1, score + 0.20);
		}
		signals.add("au moins un auteur est absent du document");
		return Math.max(0, score - 0.25);
	}

	private double doiScore(String value, PreparedDocument document, double score, List<String> signals) {
		if (DOI_PATTERN.matcher(value).matches() && document.dois().contains(value)) {
			signals.add("doi valide et detecte localement");
			return Math.min(1, score + 0.25);
		}
		signals.add("doi invalide ou different du doi detecte");
		return Math.max(0, score - 0.40);
	}

	private double dateScore(String value, double score, List<String> signals) {
		try {
			int year = Integer.parseInt(value.substring(0, 4));
			if (year >= 1900 && year <= Year.now().getValue() + 1) {
				signals.add("date plausible");
				return Math.min(1, score + 0.15);
			}
		} catch (RuntimeException ignored) {
			// The invalid value is reported below.
		}
		signals.add("date hors plage plausible");
		return Math.max(0, score - 0.25);
	}

	private double languageScore(String value, PreparedDocument document, double score, List<String> signals) {
		if (value.equalsIgnoreCase(document.language())) {
			signals.add("langue confirmee par la detection locale");
			return Math.min(1, score + 0.20);
		}
		signals.add("langue differente de la detection locale");
		return Math.max(0, score - 0.25);
	}

	private double vocabularyScore(String value, Set<String> vocabulary, double score, List<String> signals) {
		if (vocabulary.contains(normalized(value))) {
			signals.add("valeur du vocabulaire controle");
			return Math.min(1, score + 0.10);
		}
		signals.add("valeur hors vocabulaire controle");
		return 0;
	}

	private String normalized(String value) {
		return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}

	private double clamp(double score) {
		return Math.max(0, Math.min(1, score));
	}
}

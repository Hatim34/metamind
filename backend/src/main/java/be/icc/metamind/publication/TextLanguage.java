package be.icc.metamind.publication;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Devine la langue reelle d'un titre et d'un resume.
 *
 * La langue enregistree d'une notice decrit le document, et elle peut etre fausse ou
 * differente de celle de ses metadonnees (article neerlandais avec un resume anglais).
 * Pour decider s'il faut traduire, on regarde donc le texte affiche lui-meme.
 */
final class TextLanguage {
	private static final Map<String, Set<String>> MARKERS = Map.of(
			"fr", Set.of("les", "des", "est", "une", "dans", "pour", "cette", "sont", "aux", "nous", "qui", "plus", "du", "la", "le", "et", "sur", "au", "par", "entre", "leur"),
			"nl", Set.of("het", "een", "van", "zijn", "wordt", "deze", "niet", "ook", "maar", "worden", "bij", "naar", "voor", "met", "als", "dat", "over", "tussen", "hun"),
			"en", Set.of("the", "and", "this", "that", "with", "from", "are", "was", "which", "have", "been", "their", "of", "for", "on", "to", "is", "between", "its")
	);

	private TextLanguage() {
	}

	/** Langue dominante, ou null si le texte est trop court ou ambigu. */
	static String guess(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		String[] words = text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
		String best = null;
		long bestScore = 0;
		long secondScore = 0;
		for (Map.Entry<String, Set<String>> entry : MARKERS.entrySet()) {
			long score = Arrays.stream(words).filter(entry.getValue()::contains).count();
			if (score > bestScore) {
				secondScore = bestScore;
				bestScore = score;
				best = entry.getKey();
			} else if (score > secondScore) {
				secondScore = score;
			}
		}
		return bestScore >= 2 && bestScore >= secondScore + 2 ? best : null;
	}
}

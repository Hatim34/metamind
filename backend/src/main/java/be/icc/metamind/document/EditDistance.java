package be.icc.metamind.document;

import java.util.Locale;

/**
 * Distance de Levenshtein normalisee entre une suggestion et la valeur finalement publiee.
 * 0.0 signifie que le bibliothecaire n'a rien change, 1.0 qu'il a tout reecrit.
 */
public final class EditDistance {
	/** Au dela de cette longueur les valeurs sont tronquees : la comparaison reste O(n*m). */
	private static final int MAX_COMPARED_CHARS = 1_000;

	private EditDistance() {
	}

	/** Normalise comme la comparaison de decision : minuscules, espaces reduits, extremites coupees. */
	public static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	/**
	 * Retourne la distance normalisee entre deux valeurs deja comparables.
	 * Deux valeurs vides sont considerees identiques (0.0).
	 */
	public static double normalized(String left, String right) {
		String a = truncate(normalize(left));
		String b = truncate(normalize(right));
		if (a.isEmpty() && b.isEmpty()) {
			return 0.0;
		}
		if (a.equals(b)) {
			return 0.0;
		}
		int distance = levenshtein(a, b);
		int longest = Math.max(a.length(), b.length());
		return longest == 0 ? 0.0 : (double) distance / longest;
	}

	private static String truncate(String value) {
		return value.length() <= MAX_COMPARED_CHARS ? value : value.substring(0, MAX_COMPARED_CHARS);
	}

	private static int levenshtein(String a, String b) {
		int[] previous = new int[b.length() + 1];
		int[] current = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			previous[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			current[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
				int deletion = previous[j] + 1;
				int insertion = current[j - 1] + 1;
				current[j] = Math.min(substitution, Math.min(deletion, insertion));
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}
		return previous[b.length()];
	}
}

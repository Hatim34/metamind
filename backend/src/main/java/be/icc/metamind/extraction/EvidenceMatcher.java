package be.icc.metamind.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Retrouve dans le texte du document la preuve d'une valeur proposee par le modele.
 *
 * Une recherche litterale echouait presque toujours : un titre ou un resume est coupe
 * par des retours a la ligne dans le PDF, les guillemets et tirets typographiques
 * varient, et une liste d'auteurs ou de mots-cles n'apparait jamais sous la forme
 * "a, b, c". La confiance de ces champs restait alors plafonnee, meme pour une copie
 * exacte du document.
 */
final class EvidenceMatcher {
	private static final Set<String> MULTI_VALUED_FIELDS = Set.of("auteurs", "mots_cles");
	/** Au-dela, une valeur est comparee par fenetres de mots : une copie presque exacte suffit. */
	private static final int LONG_VALUE_CHARS = 200;
	private static final int WINDOW_WORDS = 8;
	private static final double LONG_VALUE_COVERAGE = 0.7;
	private static final int EVIDENCE_CHARS = 200;

	private EvidenceMatcher() {
	}

	/** Forme comparable : minuscules, espaces reduits, guillemets et tirets unifies. */
	static String normalize(String value) {
		return Normalized.of(value).text().trim();
	}

	/**
	 * Extrait du document qui prouve la valeur, tel qu'il est ecrit, ou null s'il n'y en a pas.
	 * Pour un champ multivalue, chaque element doit figurer dans le document.
	 */
	static String find(String field, String value, String documentText) {
		if (value == null || value.isBlank()) {
			return null;
		}
		Normalized text = Normalized.of(documentText);
		if ("date_publication".equals(field)) {
			// Le modele ecrit AAAA-MM-JJ ; le document ecrit "2025" ou "27 oktober 2025" : on cherche l'annee.
			return value.length() >= 4 ? text.snippet(text.text().indexOf(value.substring(0, 4))) : null;
		}
		if (!MULTI_VALUED_FIELDS.contains(field)) {
			return evidenceOf(normalize(value), text);
		}
		List<String> items = Arrays.stream(value.split("[,;]"))
				.map(EvidenceMatcher::normalize)
				.filter(item -> !item.isEmpty())
				.toList();
		String first = null;
		for (String item : items) {
			String evidence = text.snippet(text.text().indexOf(item));
			if (evidence == null) {
				return null;
			}
			first = first == null ? evidence : first;
		}
		return first;
	}

	private static String evidenceOf(String value, Normalized text) {
		String exact = text.snippet(text.text().indexOf(value));
		if (exact != null || value.length() <= LONG_VALUE_CHARS) {
			return exact;
		}
		List<String> windows = windows(value);
		String first = null;
		int found = 0;
		for (String window : windows) {
			String evidence = text.snippet(text.text().indexOf(window));
			if (evidence != null) {
				found++;
				first = first == null ? evidence : first;
			}
		}
		return found >= Math.ceil(windows.size() * LONG_VALUE_COVERAGE) ? first : null;
	}

	private static List<String> windows(String value) {
		String[] words = value.split(" ");
		List<String> windows = new ArrayList<>();
		for (int start = 0; start < words.length; start += WINDOW_WORDS) {
			int end = Math.min(words.length, start + WINDOW_WORDS);
			windows.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
		}
		return windows;
	}

	/** Texte normalise, avec pour chaque caractere sa position dans le texte original. */
	private record Normalized(String original, String text, int[] origins) {
		static Normalized of(String original) {
			String source = original == null ? "" : original;
			StringBuilder text = new StringBuilder(source.length());
			int[] origins = new int[source.length()];
			for (int index = 0; index < source.length(); index++) {
				char character = canonical(source.charAt(index));
				if (character == ' ' && (text.isEmpty() || text.charAt(text.length() - 1) == ' ')) {
					continue;
				}
				origins[text.length()] = index;
				text.append(character);
			}
			return new Normalized(source, text.toString(), origins);
		}

		/** Extrait original d'environ 200 caracteres a partir d'une position du texte normalise. */
		String snippet(int start) {
			if (start < 0) {
				return null;
			}
			int from = origins[start];
			return original.substring(from, Math.min(original.length(), from + EVIDENCE_CHARS));
		}

		private static char canonical(char character) {
			if (Character.isWhitespace(character) || character == '\u00A0' || character == '\u202F') {
				return ' ';
			}
			return switch (character) {
				case '\u2018', '\u2019', '\u201B', '\u2032' -> '\'';
				case '\u201C', '\u201D', '\u201E', '\u00AB', '\u00BB' -> '"';
				case '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2212' -> '-';
				default -> Character.toLowerCase(character);
			};
		}
	}
}

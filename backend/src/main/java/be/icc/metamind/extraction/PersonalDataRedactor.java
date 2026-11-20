package be.icc.metamind.extraction;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masque les donnees personnelles inutiles a l'extraction avant d'envoyer un extrait au modele.
 *
 * Minimisation (RGPD, article 5) : l'adresse email, le telephone, le compte bancaire ou le numero de
 * registre national d'un auteur ne servent a remplir aucun champ de la notice. Ils sont remplaces par
 * une balise et seul leur nombre est conserve. Les noms d'auteurs, les DOI et les ORCID restent : ce
 * sont des metadonnees attendues. Les IBAN et numeros de registre national ne sont masques que si leur
 * cle de controle est valide, pour ne pas toucher aux nombres ordinaires d'un article.
 */
public final class PersonalDataRedactor {
	public static final String EMAIL = "EMAIL";
	public static final String TELEPHONE = "TELEPHONE";
	public static final String IBAN = "IBAN";
	public static final String REGISTRE_NATIONAL = "REGISTRE_NATIONAL";

	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"(?i)\\b[a-z0-9._%+-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.[a-z]{2,}\\b");
	private static final Pattern IBAN_PATTERN = Pattern.compile(
			"\\b[A-Z]{2}\\d{2}(?:[ ]?[A-Z0-9]{4}){2,7}(?:[ ]?[A-Z0-9]{1,3})?\\b");
	private static final Pattern NATIONAL_NUMBER_PATTERN = Pattern.compile(
			"\\b\\d{2}[.]?\\d{2}[.]?\\d{2}[- ]?\\d{3}[.]?\\d{2}\\b");
	/** Numero international (+32, 0032) ou numero precede d'un mot comme tel, gsm, fax ou phone ; jamais un ORCID. */
	private static final Pattern PHONE_PATTERN = Pattern.compile(
			"(?i)(?:(?<![\\d-])(?!\\d{4}-\\d{4}-\\d{4}-[\\dX]{4})(?:\\+|\\b00)\\d{1,3}[ ./-]?(?:\\(0\\)[ ./-]?)?\\d(?:[ ./-]?\\d){6,11}"
					+ "|(?<=(?:t[ée]l|tel\\.|t[ée]l[ée]phone|phone|gsm|mobile|fax)[ :.]{0,3})0\\d(?:[ ./-]?\\d){7,9})");

	private PersonalDataRedactor() {
	}

	/** Texte a envoyer, et nombre de valeurs masquees par categorie (jamais les valeurs elles-memes). */
	public record Redaction(String text, Map<String, Integer> masked) {
		public int total() {
			return masked.values().stream().mapToInt(Integer::intValue).sum();
		}

		/** Resume lisible, par exemple « EMAIL:2, TELEPHONE:1 », ou null si rien n'a ete masque. */
		public String summary() {
			if (masked.isEmpty()) {
				return null;
			}
			StringBuilder value = new StringBuilder();
			masked.forEach((category, count) -> value.append(value.isEmpty() ? "" : ", ").append(category).append(':').append(count));
			return value.toString();
		}
	}

	public static Redaction redact(String text) {
		if (text == null || text.isBlank()) {
			return new Redaction(text, Map.of());
		}
		Map<String, Integer> masked = new LinkedHashMap<>();
		String result = replace(text, EMAIL_PATTERN, EMAIL, value -> true, masked);
		result = replace(result, IBAN_PATTERN, IBAN, PersonalDataRedactor::isValidIban, masked);
		result = replace(result, NATIONAL_NUMBER_PATTERN, REGISTRE_NATIONAL, PersonalDataRedactor::isValidNationalNumber, masked);
		result = replace(result, PHONE_PATTERN, TELEPHONE, value -> true, masked);
		return new Redaction(result, Map.copyOf(masked));
	}

	private static String replace(String text, Pattern pattern, String category, Predicate<String> accept, Map<String, Integer> masked) {
		Matcher matcher = pattern.matcher(text);
		StringBuilder output = new StringBuilder();
		while (matcher.find()) {
			if (accept.test(matcher.group())) {
				masked.merge(category, 1, Integer::sum);
				matcher.appendReplacement(output, Matcher.quoteReplacement("[" + category + "]"));
			} else {
				matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
			}
		}
		matcher.appendTail(output);
		return output.toString();
	}

	/** Cle de controle ISO 13616 : les quatre premiers caracteres passent a la fin, le tout modulo 97 vaut 1. */
	static boolean isValidIban(String value) {
		String iban = value.replace(" ", "");
		if (iban.length() < 15 || iban.length() > 34) {
			return false;
		}
		StringBuilder digits = new StringBuilder();
		for (char character : (iban.substring(4) + iban.substring(0, 4)).toCharArray()) {
			digits.append(Character.isDigit(character) ? String.valueOf(character) : String.valueOf(character - 'A' + 10));
		}
		return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
	}

	/** Numero de registre national belge : 97 moins (9 premiers chiffres modulo 97), avec un 2 devant pour les naissances depuis 2000. */
	static boolean isValidNationalNumber(String value) {
		String digits = value.replaceAll("\\D", "");
		if (digits.length() != 11) {
			return false;
		}
		long base = Long.parseLong(digits.substring(0, 9));
		int check = Integer.parseInt(digits.substring(9));
		return check == 97 - (int) (base % 97) || check == 97 - (int) ((2_000_000_000L + base) % 97);
	}
}

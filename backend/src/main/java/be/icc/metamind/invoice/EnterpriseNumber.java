package be.icc.metamind.invoice;

/**
 * Numero d'entreprise belge (BCE) : dix chiffres commencant par 0 ou 1, dont les deux derniers
 * valent 97 moins (les huit premiers modulo 97). Il sert d'identifiant Peppol (schema 0208)
 * et, precede de BE, de numero de TVA.
 */
public final class EnterpriseNumber {
	private EnterpriseNumber() {
	}

	/** « BE 0123.456.749 » devient « 0123456749 » ; les autres formes sont rendues telles quelles. */
	public static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String digits = value.toUpperCase().replace("BE", "").replaceAll("[^0-9]", "");
		return digits.length() == 9 ? "0" + digits : digits;
	}

	public static boolean isValid(String value) {
		String digits = normalize(value);
		if (digits == null || !digits.matches("[01]\\d{9}")) {
			return false;
		}
		long base = Long.parseLong(digits.substring(0, 8));
		return Integer.parseInt(digits.substring(8)) == 97 - (int) (base % 97);
	}

	/** Forme lisible : 0123.456.749. */
	public static String format(String value) {
		String digits = normalize(value);
		return digits == null || digits.length() != 10 ? value
				: digits.substring(0, 4) + "." + digits.substring(4, 7) + "." + digits.substring(7);
	}
}

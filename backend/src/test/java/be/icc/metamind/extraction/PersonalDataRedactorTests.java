package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonalDataRedactorTests {
	private static final String TEXT = """
			Croissance urbaine et logement : une etude de cas (2024)
			Marie Dubois, Jan Peeters
			Universite libre de Bruxelles. Contact : marie.dubois@ulb.be, Tel : 02 650 21 11, +32 471 23 45 67
			Compte du projet : BE68 5390 0754 7034. Registre national : 85.07.30-033.28
			DOI : 10.1016/j.urban.2024.01.002 - ORCID 0000-0002-2271-7972
			Sur 1 234 567 menages, 12,5 % vivent seuls (tableau 3, p. 45-67).
			""";

	@Test
	void masksPersonalDataThatTheExtractionDoesNotNeed() {
		PersonalDataRedactor.Redaction redaction = PersonalDataRedactor.redact(TEXT);

		assertThat(redaction.text())
				.doesNotContain("marie.dubois@ulb.be", "02 650 21 11", "+32 471 23 45 67", "BE68 5390 0754 7034", "85.07.30-033.28")
				.contains("[EMAIL]", "[TELEPHONE]", "[IBAN]", "[REGISTRE_NATIONAL]");
		assertThat(redaction.masked()).containsEntry(PersonalDataRedactor.EMAIL, 1)
				.containsEntry(PersonalDataRedactor.TELEPHONE, 2)
				.containsEntry(PersonalDataRedactor.IBAN, 1)
				.containsEntry(PersonalDataRedactor.REGISTRE_NATIONAL, 1);
		assertThat(redaction.total()).isEqualTo(5);
	}

	@Test
	void keepsTheMetadataTheModelMustFind() {
		String text = PersonalDataRedactor.redact(TEXT).text();

		assertThat(text).contains("Marie Dubois, Jan Peeters", "10.1016/j.urban.2024.01.002", "0000-0002-2271-7972",
				"(2024)", "1 234 567 menages", "p. 45-67");
	}

	@Test
	void leavesNumbersWithAnInvalidCheckDigitUntouched() {
		PersonalDataRedactor.Redaction redaction = PersonalDataRedactor.redact("Reference BE68 5390 0754 7035 et code 85.07.30-033.29.");

		assertThat(redaction.text()).contains("BE68 5390 0754 7035", "85.07.30-033.29");
		assertThat(redaction.masked()).isEmpty();
		assertThat(redaction.summary()).isNull();
	}

	@Test
	void recognisesNationalNumbersOfPeopleBornSince2000() {
		assertThat(PersonalDataRedactor.isValidNationalNumber("00.01.20-012.06")).isTrue();
	}
}

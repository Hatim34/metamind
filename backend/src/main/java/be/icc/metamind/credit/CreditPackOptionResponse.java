package be.icc.metamind.credit;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditPackOptionResponse(
		int id,

		@JsonProperty("quantite")
		int credits,

		@JsonProperty("montant_paye")
		BigDecimal amount,

		@JsonProperty("devise")
		String currency,

		@JsonProperty("libelle")
		String label
) {
}

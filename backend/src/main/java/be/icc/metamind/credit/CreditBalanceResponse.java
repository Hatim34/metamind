package be.icc.metamind.credit;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditBalanceResponse(
		@JsonProperty("institution_id")
		long institutionId,

		String institution,

		@JsonProperty("solde_credits")
		int balance
) {
}

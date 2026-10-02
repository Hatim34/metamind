package be.icc.metamind.credit;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditCheckoutStatusResponse(
		String reference,
		String status,
		@JsonProperty("solde_credits") int balance
) {
}

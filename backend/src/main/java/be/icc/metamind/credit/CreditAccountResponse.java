package be.icc.metamind.credit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditAccountResponse(
		@JsonProperty("solde")
		CreditBalanceResponse balance,

		@JsonProperty("mouvements")
		List<CreditMovementResponse> movements
) {
}

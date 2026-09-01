package be.icc.metamind.credit;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditCheckoutResponse(
		@JsonProperty("checkout_url")
		String checkoutUrl,
		String reference
) {
}

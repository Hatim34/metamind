package be.icc.metamind.credit;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record CreditCheckoutRequest(
		@JsonProperty("pack_id")
		@Min(1)
		int packId,

		@JsonProperty("cgv_acceptees")
		@AssertTrue(message = "Les conditions generales doivent etre acceptees.")
		boolean termsAccepted
) {
}

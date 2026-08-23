package be.icc.metamind.credit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreditPurchaseRequest(
		@Min(1)
		@Max(1000)
		int amount
) {
}

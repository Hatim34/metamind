package be.icc.metamind.credit;

import jakarta.validation.constraints.Size;

public record AdminCreditAdjustmentRequest(
		int amount,

		@Size(max = 255)
		String reason
) {
}

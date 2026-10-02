package be.icc.metamind.credit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreditAdjustmentRequest(
		int amount,

		@NotBlank
		@Size(max = 255)
		String reason
) {
}

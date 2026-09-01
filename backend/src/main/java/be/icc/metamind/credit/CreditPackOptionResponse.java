package be.icc.metamind.credit;

import java.math.BigDecimal;

public record CreditPackOptionResponse(
		int id,
		int credits,
		BigDecimal amount,
		String currency,
		String label
) {
}

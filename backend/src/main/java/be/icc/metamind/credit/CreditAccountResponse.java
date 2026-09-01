package be.icc.metamind.credit;

import java.util.List;

public record CreditAccountResponse(
		CreditBalanceResponse balance,
		List<CreditMovementResponse> movements
) {
}

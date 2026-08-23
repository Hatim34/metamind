package be.icc.metamind.credit;

public record CreditBalanceResponse(
		long institutionId,
		String institution,
		int balance
) {
}

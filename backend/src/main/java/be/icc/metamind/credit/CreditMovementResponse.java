package be.icc.metamind.credit;

import java.time.LocalDateTime;

public record CreditMovementResponse(
		long id,
		String institution,
		CreditMovementType type,
		int amount,
		int balanceAfter,
		String description,
		LocalDateTime createdAt
) {
	public static CreditMovementResponse from(CreditMovementEntity movement) {
		return new CreditMovementResponse(
				movement.getId(),
				movement.getInstitution().getName(),
				movement.getType(),
				movement.getAmount(),
				movement.getBalanceAfter(),
				movement.getDescription(),
				movement.getCreatedAt()
		);
	}
}

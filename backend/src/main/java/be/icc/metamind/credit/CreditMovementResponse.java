package be.icc.metamind.credit;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditMovementResponse(
		long id,
		String institution,
		CreditMovementType type,

		@JsonProperty("montant")
		int amount,

		@JsonProperty("solde_apres")
		int balanceAfter,

		String description,

		@JsonProperty("date_creation")
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

package be.icc.metamind.institution;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InstitutionResponse(
		long id,
		String code,

		@JsonProperty("nom")
		String name,

		@JsonProperty("domaine_email")
		String emailDomain,

		@JsonProperty("actif")
		boolean active,

		@JsonProperty("solde_credits")
		int creditBalance,

		@JsonProperty("achats_suspendus")
		boolean purchasesSuspended,

		@JsonProperty("credits_bienvenue_accordes")
		boolean welcomeCreditsGranted,

		@JsonProperty("en_attente")
		boolean pending
) {
	public static InstitutionResponse from(InstitutionEntity institution) {
		return new InstitutionResponse(
				institution.getId(),
				institution.getCode(),
				institution.getName(),
				institution.getEmailDomain(),
				institution.isActive(),
				institution.getCreditBalance(),
				institution.isPurchasesSuspended(),
				institution.isWelcomeCreditsGranted(),
				institution.isPending()
		);
	}
}

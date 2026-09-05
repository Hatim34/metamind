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
		int creditBalance
) {
	public static InstitutionResponse from(InstitutionEntity institution) {
		return new InstitutionResponse(
				institution.getId(),
				institution.getCode(),
				institution.getName(),
				institution.getEmailDomain(),
				institution.isActive(),
				institution.getCreditBalance()
		);
	}
}

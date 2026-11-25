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
		boolean pending,

		@JsonProperty("facturation")
		Billing billing
) {
	/** Donnees de facturation de l'institution (Peppol). */
	public record Billing(
			@JsonProperty("raison_sociale") String legalName,
			@JsonProperty("numero_entreprise") String enterpriseNumber,
			@JsonProperty("adresse_rue") String street,
			@JsonProperty("code_postal") String postalCode,
			@JsonProperty("ville") String city,
			@JsonProperty("pays") String country,
			@JsonProperty("email_facturation") String email,
			@JsonProperty("complete") boolean complete
	) {
	}

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
				institution.isPending(),
				new Billing(institution.getLegalName(), institution.getEnterpriseNumber(), institution.getStreet(),
						institution.getPostalCode(), institution.getCity(), institution.getCountry(), institution.getBillingEmail(),
						institution.hasCompleteBilling())
		);
	}
}

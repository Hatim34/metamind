package be.icc.metamind.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionBillingRequest(
		@JsonProperty("raison_sociale") @NotBlank @Size(max = 255) String legalName,
		@JsonProperty("numero_entreprise") @NotBlank @Size(max = 20) String enterpriseNumber,
		@JsonProperty("adresse_rue") @NotBlank @Size(max = 255) String street,
		@JsonProperty("code_postal") @NotBlank @Size(max = 20) String postalCode,
		@NotBlank @Size(max = 120) String ville,
		@Size(max = 2) String pays,
		@JsonProperty("email_facturation") @Email @Size(max = 255) String billingEmail
) {
}

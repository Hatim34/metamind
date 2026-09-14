package be.icc.metamind.user;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Export des donnees personnelles d'un utilisateur (droit d'acces et de
 * portabilite, art. 15 et 20 RGPD, cf. livrable 18 - Aspects juridiques).
 * Ne contient que les donnees a caractere personnel detenues sur la personne.
 */
public record PersonalDataExportResponse(
		@JsonProperty("date_export")
		Instant exportDate,

		@JsonProperty("identifiant")
		Long id,

		@JsonProperty("prenom")
		String firstName,

		@JsonProperty("nom")
		String lastName,

		String email,

		@JsonProperty("role")
		String role,

		@JsonProperty("statut")
		String status,

		@JsonProperty("institution")
		String institution,

		@JsonProperty("base_legale")
		String legalBasis,

		@JsonProperty("finalite")
		String purpose
) {
	public static PersonalDataExportResponse from(UserEntity user) {
		return new PersonalDataExportResponse(
				Instant.now(),
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getRole().name(),
				user.getStatus().name(),
				user.getInstitution().getName(),
				"Execution du contrat de service et interet legitime (securite).",
				"Creation et gestion du compte, authentification, tracabilite."
		);
	}
}

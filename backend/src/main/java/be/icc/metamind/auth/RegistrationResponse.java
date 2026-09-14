package be.icc.metamind.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import be.icc.metamind.user.UserResponse;

/**
 * Reponse d'inscription. Aucun jeton n'est emis : un compte bibliothecaire
 * doit d'abord etre valide par un administrateur avant de pouvoir se connecter
 * (defaut securise, cf. livrable 16 - Strategie de securite).
 */
public record RegistrationResponse(
		String statut,

		String message,

		@JsonProperty("utilisateur")
		UserResponse user
) {
}

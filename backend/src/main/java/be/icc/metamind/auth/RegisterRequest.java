package be.icc.metamind.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank
		@JsonAlias("prenom")
		String firstName,

		@NotBlank
		@JsonAlias("nom")
		String lastName,

		@NotBlank
		@Email
		String email,

		String institution,

		@NotBlank
		@Size(min = 8)
		@JsonAlias("mot_de_passe")
		String password,

		/** Nom de l'institution a ajouter, quand le domaine de l'adresse n'est pas encore inscrit. */
		@Size(max = 255)
		@JsonAlias("nom_institution")
		String newInstitutionName,

		/** Langue de l'interface au moment de l'inscription : celle des emails envoyes ensuite. */
		@JsonAlias("langue")
		String language
) {
	public RegisterRequest(String firstName, String lastName, String email, String institution, String password) {
		this(firstName, lastName, email, institution, password, null, null);
	}
}

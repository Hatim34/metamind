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

		@NotBlank
		String institution,

		@NotBlank
		@Size(min = 8)
		@JsonAlias("mot_de_passe")
		String password
) {
}

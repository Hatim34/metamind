package be.icc.metamind.user;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
		@NotBlank
		@JsonAlias("prenom")
		String firstName,

		@NotBlank
		@JsonAlias("nom")
		String lastName,

		@NotBlank
		String institution
) {
}

package be.icc.metamind.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank
		@Email
		String email,

		@NotBlank
		@JsonAlias("mot_de_passe")
		String password
) {
}

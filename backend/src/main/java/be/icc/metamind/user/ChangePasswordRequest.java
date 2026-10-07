package be.icc.metamind.user;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank
		@JsonAlias("mot_de_passe_actuel")
		String currentPassword,

		@NotBlank
		@Size(min = 8)
		@JsonAlias("nouveau_mot_de_passe")
		String newPassword
) {
}

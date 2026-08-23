package be.icc.metamind.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
		@NotBlank
		String firstName,

		@NotBlank
		String lastName,

		@NotBlank
		String institution
) {
}

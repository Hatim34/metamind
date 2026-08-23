package be.icc.metamind.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstitutionRequest(
		@NotBlank
		@Size(max = 40)
		String code,

		@NotBlank
		@Size(max = 160)
		String name,

		@NotBlank
		@Size(max = 120)
		@Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
		String emailDomain
) {
}

package be.icc.metamind.publication;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicationRequest(
		@NotBlank
		@Size(max = 220)
		String title,

		@NotBlank
		@Size(max = 160)
		String author,

		String institution,

		@Min(1900)
		@Max(2100)
		int year,

		@NotNull
		Visibility visibility,

		@Size(max = 20)
		List<String> keywords
) {
}

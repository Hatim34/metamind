package be.icc.metamind.publication;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicationRequest(
		@NotBlank
		@Size(max = 220)
		@JsonAlias("titre")
		String title,

		@NotBlank
		@Size(max = 160)
		@JsonAlias("auteur")
		String author,

		String institution,

		@Min(1900)
		@Max(2100)
		@JsonAlias("annee")
		int year,

		@NotNull
		@JsonAlias("visibilite")
		Visibility visibility,

		@Size(max = 20)
		@JsonAlias("mots_cles")
		List<String> keywords
) {
}

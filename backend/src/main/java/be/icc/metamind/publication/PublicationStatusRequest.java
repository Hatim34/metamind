package be.icc.metamind.publication;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;

public record PublicationStatusRequest(
		@NotNull
		@JsonAlias("statut")
		PublicationStatus status
) {
}

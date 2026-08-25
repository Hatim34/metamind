package be.icc.metamind.publication;

import jakarta.validation.constraints.NotNull;

public record PublicationStatusRequest(
		@NotNull
		PublicationStatus status
) {
}

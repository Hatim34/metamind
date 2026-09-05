package be.icc.metamind.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(
		long id,

		@JsonProperty("prenom")
		String firstName,

		@JsonProperty("nom")
		String lastName,

		String email,

		String role,

		String institution,

		@JsonProperty("institution_id")
		long institutionId,

		@JsonProperty("statut")
		UserStatus status
) {
	public static UserResponse from(UserAccount user) {
		return new UserResponse(
				user.id(),
				user.firstName(),
				user.lastName(),
				user.email(),
				user.role(),
				user.institution(),
				0,
				user.status()
		);
	}

	public static UserResponse from(UserEntity user) {
		return new UserResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getRole().name(),
				user.getInstitution().getName(),
				user.getInstitution().getId(),
				user.getStatus()
		);
	}
}

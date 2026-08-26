package be.icc.metamind.user;

public record UserResponse(
		long id,
		String firstName,
		String lastName,
		String email,
		String role,
		String institution,
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
				user.status()
		);
	}

	public static UserResponse from(UserEntity user) {
		return new UserResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				displayRole(user.getRole()),
				user.getInstitution().getName(),
				user.getStatus()
		);
	}

	private static String displayRole(UserRole role) {
		return switch (role) {
			case LIBRARIAN -> "Bibliothecaire";
			case ADMIN -> "Administrateur";
		};
	}
}

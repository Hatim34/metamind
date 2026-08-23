package be.icc.metamind.user;

public record UserAccount(
		long id,
		String firstName,
		String lastName,
		String email,
		String role,
		String institution,
		UserStatus status
) {
}

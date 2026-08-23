package be.icc.metamind.auth;

import be.icc.metamind.user.UserResponse;

public record AuthResponse(
		String token,
		UserResponse user
) {
}

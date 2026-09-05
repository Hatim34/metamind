package be.icc.metamind.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import be.icc.metamind.user.UserResponse;

public record AuthResponse(
		String token,

		@JsonProperty("expires_in")
		int expiresIn,

		@JsonProperty("utilisateur")
		UserResponse user
) {
}

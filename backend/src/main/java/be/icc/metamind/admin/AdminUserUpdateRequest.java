package be.icc.metamind.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import be.icc.metamind.user.UserRole;
import be.icc.metamind.user.UserStatus;

public record AdminUserUpdateRequest(
		UserRole role,

		@JsonProperty("statut")
		UserStatus status
) {
}

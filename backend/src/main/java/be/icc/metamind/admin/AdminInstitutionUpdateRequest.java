package be.icc.metamind.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminInstitutionUpdateRequest(
		Boolean actif,
		@JsonProperty("achatsSuspendus") Boolean purchasesSuspended
) {
}

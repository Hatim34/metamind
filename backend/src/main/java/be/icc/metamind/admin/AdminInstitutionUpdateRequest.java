package be.icc.metamind.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;

public record AdminInstitutionUpdateRequest(
		Boolean actif,
		@JsonProperty("achatsSuspendus") Boolean purchasesSuspended,
		@Size(max = 255) String nom,
		@JsonProperty("domaineEmail") @Size(max = 255) String domaineEmail
) {
	public AdminInstitutionUpdateRequest(Boolean actif, Boolean purchasesSuspended) {
		this(actif, purchasesSuspended, null, null);
	}
}

package be.icc.metamind.admin;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import be.icc.metamind.document.AuditLogEntity;

public record AuditLogResponse(
		long id,
		String action,

		@JsonProperty("type_entite")
		String entityType,

		@JsonProperty("entite_id")
		Long entityId,

		String details,

		@JsonProperty("date_creation")
		LocalDateTime createdAt
) {
	public static AuditLogResponse from(AuditLogEntity log) {
		return new AuditLogResponse(
				log.getId(),
				log.getAction(),
				log.getEntityType(),
				log.getEntityId(),
				log.getDetails(),
				log.getCreatedAt()
		);
	}
}

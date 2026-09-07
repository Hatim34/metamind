package be.icc.metamind.document;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataHistoryResponse(
		@JsonProperty("champ")
		String field,

		@JsonProperty("ancienne_valeur")
		String previousValue,

		@JsonProperty("nouvelle_valeur")
		String newValue,

		@JsonProperty("utilisateur")
		String user,

		@JsonProperty("date_modification")
		LocalDateTime modifiedAt
) {
	public static MetadataHistoryResponse from(AuditLogEntity log) {
		String[] parts = log.getDetails() == null ? new String[0] : log.getDetails().split("\\n", 3);
		return new MetadataHistoryResponse(
				value(parts, 0),
				value(parts, 1),
				value(parts, 2),
				log.getUser() == null ? null : log.getUser().getEmail(),
				log.getCreatedAt()
		);
	}

	private static String value(String[] parts, int index) {
		return parts.length > index ? parts[index] : "";
	}
}

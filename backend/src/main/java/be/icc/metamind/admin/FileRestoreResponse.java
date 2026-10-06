package be.icc.metamind.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileRestoreResponse(
		@JsonProperty("fichier")
		String fileName,

		@JsonProperty("documents_restaures")
		List<Long> restoredDocumentIds
) {
}

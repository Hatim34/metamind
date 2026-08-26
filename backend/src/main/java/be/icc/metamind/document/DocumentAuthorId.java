package be.icc.metamind.document;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DocumentAuthorId implements Serializable {
	@Column(name = "document_id")
	private Long documentId;

	@Column(name = "auteur_id")
	private Long authorId;

	protected DocumentAuthorId() {
	}

	public DocumentAuthorId(Long documentId, Long authorId) {
		this.documentId = documentId;
		this.authorId = authorId;
	}

	public Long getDocumentId() {
		return documentId;
	}

	public Long getAuthorId() {
		return authorId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DocumentAuthorId that)) {
			return false;
		}
		return Objects.equals(documentId, that.documentId) && Objects.equals(authorId, that.authorId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentId, authorId);
	}
}

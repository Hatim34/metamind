package be.icc.metamind.document;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DocumentKeywordId implements Serializable {
	@Column(name = "document_id")
	private Long documentId;

	@Column(name = "mot_cle_id")
	private Long keywordId;

	protected DocumentKeywordId() {
	}

	public DocumentKeywordId(Long documentId, Long keywordId) {
		this.documentId = documentId;
		this.keywordId = keywordId;
	}

	public Long getDocumentId() {
		return documentId;
	}

	public Long getKeywordId() {
		return keywordId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DocumentKeywordId that)) {
			return false;
		}
		return Objects.equals(documentId, that.documentId) && Objects.equals(keywordId, that.keywordId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentId, keywordId);
	}
}

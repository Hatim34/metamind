package be.icc.metamind.document;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents_mots_cles")
public class DocumentKeywordEntity {
	@EmbeddedId
	private DocumentKeywordId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("documentId")
	@JoinColumn(name = "document_id", nullable = false)
	private DocumentEntity document;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("keywordId")
	@JoinColumn(name = "mot_cle_id", nullable = false)
	private KeywordEntity keyword;

	protected DocumentKeywordEntity() {
	}

	public DocumentKeywordEntity(DocumentEntity document, KeywordEntity keyword) {
		this.document = document;
		this.keyword = keyword;
		this.id = new DocumentKeywordId(document.getId(), keyword.getId());
	}

	public DocumentKeywordId getId() {
		return id;
	}

	public DocumentEntity getDocument() {
		return document;
	}

	public KeywordEntity getKeyword() {
		return keyword;
	}
}

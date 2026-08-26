package be.icc.metamind.document;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents_auteurs")
public class DocumentAuthorEntity {
	@EmbeddedId
	private DocumentAuthorId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("documentId")
	@JoinColumn(name = "document_id", nullable = false)
	private DocumentEntity document;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("authorId")
	@JoinColumn(name = "auteur_id", nullable = false)
	private AuthorEntity author;

	@Column(name = "ordre_auteur")
	private Integer authorOrder;

	protected DocumentAuthorEntity() {
	}

	public DocumentAuthorEntity(DocumentEntity document, AuthorEntity author, Integer authorOrder) {
		this.document = document;
		this.author = author;
		this.authorOrder = authorOrder;
		this.id = new DocumentAuthorId(document.getId(), author.getId());
	}

	public DocumentAuthorId getId() {
		return id;
	}

	public DocumentEntity getDocument() {
		return document;
	}

	public AuthorEntity getAuthor() {
		return author;
	}

	public Integer getAuthorOrder() {
		return authorOrder;
	}
}

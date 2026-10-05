package be.icc.metamind.document;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Octets de la couverture d'un document, conserves en base.
 *
 * Table separee de {@code documents} a dessein : lister le catalogue ne doit jamais
 * charger les images. Le disque du conteneur n'est pas persistant en production,
 * la base l'est.
 */
@Entity
@Table(name = "couvertures_documents")
public class DocumentCoverEntity {
	@Id
	@Column(name = "document_id")
	private Long documentId;

	@Column(name = "donnees", nullable = false, columnDefinition = "bytea")
	private byte[] data;

	@Column(name = "type_mime", nullable = false, length = 100)
	private String mediaType;

	@Column(name = "taille_octets", nullable = false)
	private int sizeInBytes;

	@Column(name = "date_creation", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected DocumentCoverEntity() {
	}

	public DocumentCoverEntity(Long documentId, byte[] data, String mediaType) {
		this.documentId = documentId;
		replace(data, mediaType);
	}

	public void replace(byte[] data, String mediaType) {
		this.data = data;
		this.mediaType = mediaType;
		this.sizeInBytes = data == null ? 0 : data.length;
		this.createdAt = LocalDateTime.now();
	}

	public Long getDocumentId() {
		return documentId;
	}

	public byte[] getData() {
		return data;
	}

	public String getMediaType() {
		return mediaType;
	}

	public int getSizeInBytes() {
		return sizeInBytes;
	}
}

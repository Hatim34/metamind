package be.icc.metamind.institution;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Photo d'une institution, conservee en base comme les couvertures des documents. */
@Entity
@Table(name = "photos_institutions")
public class InstitutionPhotoEntity {
	@Id
	@Column(name = "institution_id")
	private Long institutionId;

	@Column(name = "donnees", nullable = false, columnDefinition = "bytea")
	private byte[] data;

	@Column(name = "type_mime", nullable = false, length = 100)
	private String mediaType;

	@Column(name = "taille_octets", nullable = false)
	private int sizeInBytes;

	@Column(name = "credit", length = 300)
	private String credit;

	@Column(name = "date_creation", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected InstitutionPhotoEntity() {
	}

	public InstitutionPhotoEntity(Long institutionId, byte[] data, String mediaType, String credit) {
		this.institutionId = institutionId;
		replace(data, mediaType, credit);
	}

	public void replace(byte[] data, String mediaType, String credit) {
		this.data = data;
		this.mediaType = mediaType;
		this.sizeInBytes = data == null ? 0 : data.length;
		this.credit = credit;
		this.createdAt = LocalDateTime.now();
	}

	public Long getInstitutionId() {
		return institutionId;
	}

	public byte[] getData() {
		return data;
	}

	public String getMediaType() {
		return mediaType;
	}

	public String getCredit() {
		return credit;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

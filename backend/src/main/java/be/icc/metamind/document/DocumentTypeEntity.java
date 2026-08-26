package be.icc.metamind.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "types_documents",
		uniqueConstraints = @UniqueConstraint(name = "uk_types_documents_code", columnNames = "code")
)
public class DocumentTypeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String libelle;

	protected DocumentTypeEntity() {
	}

	public DocumentTypeEntity(String code, String libelle) {
		this.code = code;
		this.libelle = libelle;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getLibelle() {
		return libelle;
	}
}

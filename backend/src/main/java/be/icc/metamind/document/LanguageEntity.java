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
		name = "langues",
		uniqueConstraints = @UniqueConstraint(name = "uk_langues_code", columnNames = "code")
)
public class LanguageEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 5)
	private String code;

	@Column(nullable = false, length = 50)
	private String libelle;

	protected LanguageEntity() {
	}

	public LanguageEntity(String code, String libelle) {
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

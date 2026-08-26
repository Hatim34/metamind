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
		name = "mots_cles",
		uniqueConstraints = @UniqueConstraint(name = "uk_mots_cles_libelle", columnNames = "libelle")
)
public class KeywordEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String libelle;

	protected KeywordEntity() {
	}

	public KeywordEntity(String libelle) {
		this.libelle = libelle;
	}

	public Long getId() {
		return id;
	}

	public String getLibelle() {
		return libelle;
	}
}

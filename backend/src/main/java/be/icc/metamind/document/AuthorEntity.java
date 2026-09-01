package be.icc.metamind.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auteurs")
public class AuthorEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nom_complet", nullable = false, length = 255)
	private String fullName;

	@Column(length = 19)
	private String orcid;

	protected AuthorEntity() {
	}

	public AuthorEntity(String fullName, String orcid) {
		this.fullName = fullName;
		this.orcid = orcid;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getOrcid() {
		return orcid;
	}

	public void updateOrcid(String orcid) {
		if (orcid != null && !orcid.isBlank()) {
			this.orcid = orcid;
		}
	}
}

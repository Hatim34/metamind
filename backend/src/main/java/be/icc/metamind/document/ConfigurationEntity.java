package be.icc.metamind.document;

import java.time.LocalDateTime;

import be.icc.metamind.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "configurations")
public class ConfigurationEntity {
	@Id
	@Column(length = 100)
	private String cle;

	@Column(columnDefinition = "text")
	private String valeur;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "modifie_par")
	private UserEntity modifiedBy;

	@Column(name = "date_modification", nullable = false)
	private LocalDateTime modifiedAt = LocalDateTime.now();

	protected ConfigurationEntity() {
	}

	public ConfigurationEntity(String cle, String valeur, UserEntity modifiedBy) {
		this.cle = cle;
		this.valeur = valeur;
		this.modifiedBy = modifiedBy;
	}
}

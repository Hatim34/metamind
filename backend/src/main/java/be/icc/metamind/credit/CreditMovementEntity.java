package be.icc.metamind.credit;

import java.time.LocalDateTime;

import be.icc.metamind.document.EnrichmentEntity;
import be.icc.metamind.institution.InstitutionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
		name = "mouvements_credits",
		indexes = @Index(name = "idx_mouvements_credits_institution_date", columnList = "institution_id, date_mouvement")
)
public class CreditMovementEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CreditMovementType type;

	@Column(name = "quantite", nullable = false)
	private int amount;

	@jakarta.persistence.Transient
	private int balanceAfter;

	@jakarta.persistence.Transient
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "enrichissement_id")
	private EnrichmentEntity enrichment;

	@Column(name = "date_mouvement", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected CreditMovementEntity() {
	}

	public CreditMovementEntity(InstitutionEntity institution, CreditMovementType type, int amount, int balanceAfter, String description) {
		this.institution = institution;
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.description = description;
	}

	public CreditMovementEntity(InstitutionEntity institution, CreditMovementType type, int amount, EnrichmentEntity enrichment) {
		this.institution = institution;
		this.type = type;
		this.amount = amount;
		this.enrichment = enrichment;
		this.balanceAfter = institution.getCreditBalance();
		this.description = type == CreditMovementType.ACHAT ? "Achat de credits" : "Extraction de metadonnees";
	}

	public Long getId() {
		return id;
	}

	public InstitutionEntity getInstitution() {
		return institution;
	}

	public CreditMovementType getType() {
		return type;
	}

	public int getAmount() {
		return amount;
	}

	public int getBalanceAfter() {
		return balanceAfter;
	}

	public String getDescription() {
		return description == null ? "" : description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

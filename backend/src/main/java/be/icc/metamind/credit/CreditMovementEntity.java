package be.icc.metamind.credit;

import java.time.LocalDateTime;

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
		name = "credit_movements",
		indexes = @Index(name = "idx_credit_movements_institution_date", columnList = "institution_id, created_at")
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

	@Column(nullable = false)
	private int amount;

	@Column(name = "balance_after", nullable = false)
	private int balanceAfter;

	@Column(nullable = false, length = 220)
	private String description;

	@Column(name = "created_at", nullable = false)
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
		return description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

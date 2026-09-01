package be.icc.metamind.document;

import java.math.BigDecimal;
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
		name = "packs_credits",
		indexes = @Index(name = "idx_packs_credits_institution_id", columnList = "institution_id")
)
public class CreditPackEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	@Column(nullable = false)
	private int quantite;

	@Column(name = "montant_paye", precision = 10, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "reference_paiement", length = 255)
	private String paymentReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private CreditPackStatus status;

	@Column(name = "date_achat", nullable = false)
	private LocalDateTime purchasedAt = LocalDateTime.now();

	protected CreditPackEntity() {
	}

	public CreditPackEntity(InstitutionEntity institution, int quantite, BigDecimal paidAmount, String paymentReference, CreditPackStatus status) {
		this.institution = institution;
		this.quantite = quantite;
		this.paidAmount = paidAmount;
		this.paymentReference = paymentReference;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public InstitutionEntity getInstitution() {
		return institution;
	}

	public int getQuantite() {
		return quantite;
	}

	public BigDecimal getPaidAmount() {
		return paidAmount;
	}

	public String getPaymentReference() {
		return paymentReference;
	}

	public CreditPackStatus getStatus() {
		return status;
	}

	public LocalDateTime getPurchasedAt() {
		return purchasedAt;
	}

	public boolean isPaid() {
		return status == CreditPackStatus.PAYE;
	}

	public void markPaid() {
		status = CreditPackStatus.PAYE;
	}

	public void markFailed() {
		status = CreditPackStatus.ECHEC;
	}
}

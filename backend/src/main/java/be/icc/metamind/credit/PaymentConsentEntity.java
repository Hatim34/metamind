package be.icc.metamind.credit;

import java.time.Instant;

import be.icc.metamind.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "consentements_paiement")
public class PaymentConsentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ConsentType type;

	@Column(name = "version_document", nullable = false, length = 40)
	private String documentVersion;

	@Column(name = "accepte_at", nullable = false)
	private Instant acceptedAt;

	@Column(name = "ip", nullable = false, length = 64)
	private String ip;

	protected PaymentConsentEntity() {
	}

	public PaymentConsentEntity(UserEntity user, ConsentType type, String documentVersion, Instant acceptedAt, String ip) {
		this.user = user;
		this.type = type;
		this.documentVersion = documentVersion;
		this.acceptedAt = acceptedAt;
		this.ip = ip;
	}
}

package be.icc.metamind.institution;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "institutions",
		uniqueConstraints = @UniqueConstraint(name = "uk_institutions_domaine_email", columnNames = "domaine_email")
)
public class InstitutionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nom", nullable = false, length = 255)
	private String name;

	@Column(name = "domaine_email", length = 255)
	private String emailDomain;

	@Transient
	private boolean active = true;

	@Column(name = "solde_credits", nullable = false)
	private int creditBalance;

	@Column(name = "date_creation", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected InstitutionEntity() {
	}

	public InstitutionEntity(String code, String name, String emailDomain) {
		this.name = name;
		this.emailDomain = emailDomain;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		if (emailDomain == null) {
			return "";
		}
		String normalized = emailDomain.replace(".example", "").toUpperCase();
		if (normalized.startsWith("INSTITUTION-")) {
			return "INST-" + normalized.substring("INSTITUTION-".length());
		}
		if (normalized.equals("METAMIND")) {
			return "META";
		}
		return normalized;
	}

	public String getName() {
		return name;
	}

	public String getEmailDomain() {
		return emailDomain;
	}

	public boolean isActive() {
		return active;
	}

	public int getCreditBalance() {
		return creditBalance;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void addCredits(int amount) {
		creditBalance += amount;
	}

	public boolean hasCredits() {
		return creditBalance > 0;
	}

	public void consumeCredit() {
		creditBalance -= 1;
	}

	public void deactivate() {
		active = false;
	}
}

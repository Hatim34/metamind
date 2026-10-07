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
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "solde_credits >= 0")
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

	@Column(name = "actif", nullable = false)
	private boolean active = true;

	/** Demandee a l'inscription, en attente de la decision de l'administrateur. */
	@Column(name = "en_attente", nullable = false, columnDefinition = "boolean default false")
	private boolean pending;

	@Column(name = "solde_credits", nullable = false)
	private int creditBalance;

	@Column(name = "credits_bienvenue_accordes", nullable = false, columnDefinition = "boolean default false")
	private boolean welcomeCreditsGranted;

	@Column(name = "achats_suspendus", nullable = false, columnDefinition = "boolean default false")
	private boolean purchasesSuspended;

	@Version
	private long version;

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

	public void activate() {
		active = true;
		pending = false;
	}

	public boolean isPending() {
		return pending;
	}

	/** Institution demandee par une personne a son inscription : inactive tant qu'elle n'est pas validee. */
	public static InstitutionEntity requested(String name, String emailDomain) {
		InstitutionEntity institution = new InstitutionEntity(null, name, emailDomain);
		institution.active = false;
		institution.pending = true;
		return institution;
	}

	public void refuseRequest() {
		active = false;
		pending = false;
	}

	public int getCreditBalance() {
		return creditBalance;
	}

	public boolean isWelcomeCreditsGranted() {
		return welcomeCreditsGranted;
	}

	public boolean isPurchasesSuspended() {
		return purchasesSuspended;
	}

	public void suspendPurchases(boolean suspended) {
		purchasesSuspended = suspended;
	}

	public boolean grantWelcomeCredits(int amount) {
		if (welcomeCreditsGranted) {
			return false;
		}
		addCredits(amount);
		welcomeCreditsGranted = true;
		return true;
	}

	public void markWelcomeCreditsGranted() {
		welcomeCreditsGranted = true;
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
		if (!hasCredits()) {
			throw new IllegalStateException("Le solde de credits ne peut pas devenir negatif.");
		}
		creditBalance -= 1;
	}

	public void deactivate() {
		active = false;
		pending = false;
	}
}

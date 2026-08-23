package be.icc.metamind.institution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "institutions",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_institutions_code", columnNames = "code"),
				@UniqueConstraint(name = "uk_institutions_email_domain", columnNames = "email_domain")
		}
)
public class InstitutionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "email_domain", nullable = false, length = 120)
	private String emailDomain;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "credit_balance", nullable = false)
	private int creditBalance;

	protected InstitutionEntity() {
	}

	public InstitutionEntity(String code, String name, String emailDomain) {
		this.code = code;
		this.name = name;
		this.emailDomain = emailDomain;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
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

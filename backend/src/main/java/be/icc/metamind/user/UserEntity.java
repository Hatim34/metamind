package be.icc.metamind.user;

import be.icc.metamind.institution.InstitutionEntity;
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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "prenom", length = 100)
	private String firstName;

	@Column(name = "nom", length = 100)
	private String lastName;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "mot_de_passe_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status = UserStatus.ACTIF;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	@Transient
	private String displayName;

	protected UserEntity() {
	}

	public UserEntity(String firstName, String lastName, String email, String passwordHash, UserRole role, InstitutionEntity institution) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.institution = institution;
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public InstitutionEntity getInstitution() {
		return institution;
	}

	public void updateProfile(String firstName, String lastName, InstitutionEntity institution) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.institution = institution;
	}

	public void updateAdministration(UserRole role, UserStatus status) {
		if (role != null) {
			this.role = role;
		}
		if (status != null) {
			this.status = status;
		}
	}

	public void deactivate() {
		status = UserStatus.DESACTIVE;
	}

	public void anonymizeAndDeactivate() {
		status = UserStatus.DESACTIVE;
		firstName = "Compte";
		lastName = "Supprime";
		email = "compte-supprime-" + id + "@metamind.local";
		passwordHash = "compte-desactive";
	}
}

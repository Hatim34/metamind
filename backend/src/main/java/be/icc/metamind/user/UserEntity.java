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

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 80)
	private String lastName;

	@Column(nullable = false, length = 160)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
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

	public void deactivate() {
		status = UserStatus.DESACTIVE;
	}
}

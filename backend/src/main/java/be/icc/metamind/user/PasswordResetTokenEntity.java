package be.icc.metamind.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	protected PasswordResetTokenEntity() {
	}

	public PasswordResetTokenEntity(String tokenHash, UserEntity user, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.user = user;
		this.expiresAt = expiresAt;
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && expiresAt.isAfter(now);
	}

	public UserEntity getUser() {
		return user;
	}

	public void markUsed(Instant now) {
		usedAt = now;
	}
}

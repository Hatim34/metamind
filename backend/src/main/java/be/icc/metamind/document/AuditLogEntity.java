package be.icc.metamind.document;

import java.time.LocalDateTime;

import be.icc.metamind.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
		name = "logs_audit",
		indexes = {
				@Index(name = "idx_logs_audit_user_id", columnList = "user_id"),
				@Index(name = "idx_logs_audit_date_creation", columnList = "date_creation")
		}
)
public class AuditLogEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private UserEntity user;

	@Column(length = 50)
	private String action;

	@Column(name = "type_entite", length = 50)
	private String entityType;

	@Column(name = "entite_id")
	private Long entityId;

	@Column(columnDefinition = "text")
	private String details;

	@Column(name = "adresse_ip", length = 45)
	private String ipAddress;

	@Column(name = "date_creation", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected AuditLogEntity() {
	}

	public AuditLogEntity(UserEntity user, String action, String entityType, Long entityId, String details, String ipAddress) {
		this.user = user;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
		this.ipAddress = ipAddress;
	}

	public Long getId() {
		return id;
	}

	public UserEntity getUser() {
		return user;
	}

	public String getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public String getDetails() {
		return details;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

package be.icc.metamind.document;

import java.time.LocalDateTime;

import be.icc.metamind.user.UserEntity;
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
		name = "enrichissements",
		indexes = @Index(name = "idx_enrichissements_document_id", columnList = "document_id")
)
public class EnrichmentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private DocumentEntity document;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lance_par")
	private UserEntity launchedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private EnrichmentStatus status;

	@Column(name = "modele_llm", length = 100)
	private String model;

	@Column(name = "version_prompt", length = 50)
	private String promptVersion;

	@Column(name = "reponse_brute", columnDefinition = "text")
	private String rawResponse;

	@Column(name = "message_erreur", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "date_debut")
	private LocalDateTime startedAt;

	@Column(name = "date_fin")
	private LocalDateTime finishedAt;

	protected EnrichmentEntity() {
	}

	public EnrichmentEntity(DocumentEntity document, UserEntity launchedBy, EnrichmentStatus status, String model, String promptVersion) {
		this.document = document;
		this.launchedBy = launchedBy;
		this.status = status;
		this.model = model;
		this.promptVersion = promptVersion;
		this.startedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public DocumentEntity getDocument() {
		return document;
	}

	public UserEntity getLaunchedBy() {
		return launchedBy;
	}

	public EnrichmentStatus getStatus() {
		return status;
	}

	public String getModel() {
		return model;
	}

	public String getPromptVersion() {
		return promptVersion;
	}

	public String getRawResponse() {
		return rawResponse;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public void markCompleted(String rawResponse) {
		this.status = EnrichmentStatus.TERMINE;
		this.rawResponse = rawResponse;
		this.finishedAt = LocalDateTime.now();
	}

	public void markFailed(String errorMessage) {
		this.status = EnrichmentStatus.ECHEC;
		this.errorMessage = errorMessage;
		this.finishedAt = LocalDateTime.now();
	}
}

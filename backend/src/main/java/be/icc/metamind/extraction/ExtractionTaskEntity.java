package be.icc.metamind.extraction;

import java.time.LocalDateTime;

import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.EnrichmentEntity;
import be.icc.metamind.institution.InstitutionEntity;
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
import jakarta.persistence.Version;

@Entity
@Table(name = "taches_extraction", indexes = {
		@Index(name = "idx_taches_extraction_statut_prochaine_tentative", columnList = "statut, prochaine_tentative_at"),
		@Index(name = "idx_taches_extraction_lot_id", columnList = "lot_id")
})
public class ExtractionTaskEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private DocumentEntity document;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "enrichissement_id")
	private EnrichmentEntity enrichment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	@Column(name = "lot_id", nullable = false, length = 36)
	private String batchId;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private ExtractionTaskStatus status = ExtractionTaskStatus.EN_FILE;

	@Column(name = "priorite", nullable = false)
	private int priority;

	@Column(name = "tentatives", nullable = false)
	private int attempts;

	@Column(name = "prochaine_tentative_at")
	private LocalDateTime nextAttemptAt;

	@Column(name = "verrouille_par", length = 100)
	private String lockedBy;

	@Column(name = "verrouille_at")
	private LocalDateTime lockedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cree_par")
	private UserEntity createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	@Version
	private long version;

	protected ExtractionTaskEntity() {
	}

	public ExtractionTaskEntity(DocumentEntity document, InstitutionEntity institution, String batchId, UserEntity createdBy) {
		this.document = document;
		this.institution = institution;
		this.batchId = batchId;
		this.createdBy = createdBy;
	}

	public Long getId() { return id; }
	public DocumentEntity getDocument() { return document; }
	public EnrichmentEntity getEnrichment() { return enrichment; }
	public InstitutionEntity getInstitution() { return institution; }
	public String getBatchId() { return batchId; }
	public ExtractionTaskStatus getStatus() { return status; }
	public int getAttempts() { return attempts; }
	public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }

	public void start(String workerId) {
		status = ExtractionTaskStatus.EN_COURS;
		lockedBy = workerId;
		lockedAt = LocalDateTime.now();
		updatedAt = lockedAt;
	}

	public void complete(EnrichmentEntity enrichment) {
		this.enrichment = enrichment;
		status = ExtractionTaskStatus.TERMINE;
		updatedAt = LocalDateTime.now();
	}

	public void failOrRetry(LocalDateTime retryAt) {
		attempts++;
		nextAttemptAt = retryAt;
		status = retryAt == null ? ExtractionTaskStatus.ECHEC : ExtractionTaskStatus.EN_FILE;
		updatedAt = LocalDateTime.now();
	}

	public boolean cancel() {
		if (status != ExtractionTaskStatus.EN_FILE) {
			return false;
		}
		status = ExtractionTaskStatus.ANNULE;
		updatedAt = LocalDateTime.now();
		return true;
	}
}

package be.icc.metamind.document;

import java.time.LocalDate;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "metadonnees",
		uniqueConstraints = @UniqueConstraint(name = "uk_metadonnees_document", columnNames = "document_id")
)
public class MetadataEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private DocumentEntity document;

	@Column(length = 500)
	private String titre;

	@Column(columnDefinition = "text")
	private String resume;

	@Column(name = "date_publication")
	private LocalDate publicationDate;

	@Column(length = 255)
	private String classification;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "langue_id")
	private LanguageEntity language;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_document_id")
	private DocumentTypeEntity documentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private MetadataStatus status = MetadataStatus.EN_ATTENTE;

	@Column(name = "date_validation")
	private LocalDateTime validatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "validee_par")
	private UserEntity validatedBy;

	@Column(name = "date_generation", nullable = false)
	private LocalDateTime generatedAt = LocalDateTime.now();

	protected MetadataEntity() {
	}

	public MetadataEntity(DocumentEntity document, String titre, String resume, LocalDate publicationDate, String classification, MetadataStatus status) {
		this.document = document;
		this.titre = titre;
		this.resume = resume;
		this.publicationDate = publicationDate;
		this.classification = classification;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public DocumentEntity getDocument() {
		return document;
	}

	public String getTitre() {
		return titre;
	}

	public String getResume() {
		return resume;
	}

	public LocalDate getPublicationDate() {
		return publicationDate;
	}

	public String getClassification() {
		return classification;
	}

	public LanguageEntity getLanguage() {
		return language;
	}

	public DocumentTypeEntity getDocumentType() {
		return documentType;
	}

	public MetadataStatus getStatus() {
		return status;
	}

	public LocalDateTime getValidatedAt() {
		return validatedAt;
	}

	public UserEntity getValidatedBy() {
		return validatedBy;
	}

	public LocalDateTime getGeneratedAt() {
		return generatedAt;
	}

	public void markGenerated(String titre, String resume) {
		this.titre = titre;
		this.resume = resume;
		this.status = MetadataStatus.EN_ATTENTE;
		this.generatedAt = LocalDateTime.now();
	}

	public void markGenerated(String titre, String resume, String classification) {
		this.titre = titre;
		this.resume = resume;
		this.classification = classification;
		this.status = MetadataStatus.EN_ATTENTE;
		this.generatedAt = LocalDateTime.now();
	}
}

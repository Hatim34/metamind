package be.icc.metamind.publication;

import java.time.LocalDateTime;

import be.icc.metamind.document.DocumentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Traduction cachee d'une notice dans une langue cible. */
@Entity
@Table(
		name = "traductions_publications",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_traductions_publications_document_langue",
				columnNames = { "document_id", "langue_cible" }
		)
)
public class PublicationTranslationEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private DocumentEntity document;

	@Column(name = "langue_cible", nullable = false, length = 5)
	private String targetLanguage;

	@Column(name = "empreinte_source", nullable = false, length = 64)
	private String sourceFingerprint;

	@Column(name = "titre", length = 500)
	private String title;

	@Column(name = "resume", columnDefinition = "text")
	private String summary;

	@Column(name = "mots_cles", columnDefinition = "text")
	private String keywordsJson;

	@Column(name = "modele", length = 100)
	private String model;

	@Column(name = "date_creation", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "classification", length = 255)
	private String classification;

	protected PublicationTranslationEntity() {
	}

	public PublicationTranslationEntity(
			DocumentEntity document,
			String targetLanguage,
			String sourceFingerprint,
			String title,
			String summary,
			String keywordsJson,
			String model
	) {
		this.document = document;
		this.targetLanguage = targetLanguage;
		refresh(sourceFingerprint, title, summary, keywordsJson, model);
	}

	public void translateClassification(String classification) {
		this.classification = classification;
	}

	public String getClassification() {
		return classification;
	}

	public void refresh(String sourceFingerprint, String title, String summary, String keywordsJson, String model) {
		this.sourceFingerprint = sourceFingerprint;
		this.title = title;
		this.summary = summary;
		this.keywordsJson = keywordsJson;
		this.model = model;
		this.createdAt = LocalDateTime.now();
	}

	public DocumentEntity getDocument() {
		return document;
	}

	public String getSourceFingerprint() {
		return sourceFingerprint;
	}

	public String getTitle() {
		return title;
	}

	public String getSummary() {
		return summary;
	}

	public String getKeywordsJson() {
		return keywordsJson;
	}
}

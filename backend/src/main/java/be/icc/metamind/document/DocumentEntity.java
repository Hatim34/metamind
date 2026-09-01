package be.icc.metamind.document;

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

@Entity
@Table(
		name = "documents",
		indexes = {
				@Index(name = "idx_documents_institution_id", columnList = "institution_id"),
				@Index(name = "idx_documents_statut_visibilite", columnList = "statut, visibilite")
		}
)
public class DocumentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nom_fichier", nullable = false, length = 255)
	private String fileName;

	@Column(name = "chemin_fichier", length = 500)
	private String filePath;

	@Column(name = "taille_fichier")
	private Long fileSize;

	@Column(name = "type_mime", length = 100)
	private String mimeType;

	@Column(name = "texte_extrait", columnDefinition = "text")
	private String extractedText;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private DocumentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibilite", nullable = false, length = 20)
	private DocumentVisibility visibility;

	@Column(name = "search_vector", columnDefinition = "text")
	private String searchVector;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "importe_par")
	private UserEntity importedBy;

	protected DocumentEntity() {
	}

	public DocumentEntity(String fileName, String filePath, Long fileSize, String mimeType, String extractedText, DocumentStatus status, DocumentVisibility visibility, InstitutionEntity institution, UserEntity importedBy) {
		this.fileName = fileName;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.mimeType = mimeType;
		this.extractedText = extractedText;
		this.status = status;
		this.visibility = visibility;
		this.institution = institution;
		this.importedBy = importedBy;
	}

	public Long getId() {
		return id;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getExtractedText() {
		return extractedText;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public DocumentVisibility getVisibility() {
		return visibility;
	}

	public String getSearchVector() {
		return searchVector;
	}

	public InstitutionEntity getInstitution() {
		return institution;
	}

	public UserEntity getImportedBy() {
		return importedBy;
	}

	public void markExtractionCompleted(String searchText) {
		this.status = DocumentStatus.A_VALIDER;
		this.searchVector = searchText;
	}

	public void markExtractionFailed() {
		this.status = DocumentStatus.EN_ATTENTE;
	}

	public void updateStatus(DocumentStatus status) {
		this.status = status;
	}

	public void publish(DocumentVisibility visibility, String searchText) {
		this.status = DocumentStatus.PUBLIE;
		this.visibility = visibility;
		this.searchVector = searchText;
	}
}

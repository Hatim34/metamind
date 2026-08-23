package be.icc.metamind.publication;

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

@Entity
@Table(name = "publications")
public class PublicationEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 220)
	private String title;

	@Column(nullable = false, length = 160)
	private String author;

	@Column(name = "publication_year", nullable = false)
	private int year;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PublicationStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Visibility visibility;

	@Column(name = "keywords_text", nullable = false, length = 500)
	private String keywordsText;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	protected PublicationEntity() {
	}

	public PublicationEntity(String title, String author, int year, PublicationStatus status, Visibility visibility, String keywordsText, InstitutionEntity institution) {
		this.title = title;
		this.author = author;
		this.year = year;
		this.status = status;
		this.visibility = visibility;
		this.keywordsText = keywordsText;
		this.institution = institution;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public int getYear() {
		return year;
	}

	public PublicationStatus getStatus() {
		return status;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public String getKeywordsText() {
		return keywordsText;
	}

	public InstitutionEntity getInstitution() {
		return institution;
	}

	public void markExtractionCompleted(String keywordsText) {
		this.status = PublicationStatus.A_VALIDER;
		this.keywordsText = keywordsText;
	}
}

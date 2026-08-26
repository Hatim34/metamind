package be.icc.metamind.document;

import java.math.BigDecimal;

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
		name = "suggestions_metadonnees",
		indexes = @Index(name = "idx_suggestions_enrichissement_id", columnList = "enrichissement_id")
)
public class MetadataSuggestionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "enrichissement_id", nullable = false)
	private EnrichmentEntity enrichment;

	@Column(nullable = false, length = 50)
	private String champ;

	@Column(name = "valeur_suggeree", columnDefinition = "text")
	private String suggestedValue;

	@Column(name = "score_confiance", precision = 3, scale = 2)
	private BigDecimal confidenceScore;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private SuggestionDecision decision;

	@Column(name = "valeur_finale", columnDefinition = "text")
	private String finalValue;

	protected MetadataSuggestionEntity() {
	}

	public MetadataSuggestionEntity(EnrichmentEntity enrichment, String champ, String suggestedValue, BigDecimal confidenceScore) {
		this.enrichment = enrichment;
		this.champ = champ;
		this.suggestedValue = suggestedValue;
		this.confidenceScore = confidenceScore;
	}

	public Long getId() {
		return id;
	}

	public EnrichmentEntity getEnrichment() {
		return enrichment;
	}

	public String getChamp() {
		return champ;
	}

	public String getSuggestedValue() {
		return suggestedValue;
	}

	public BigDecimal getConfidenceScore() {
		return confidenceScore;
	}

	public SuggestionDecision getDecision() {
		return decision;
	}

	public String getFinalValue() {
		return finalValue;
	}
}

package be.icc.metamind.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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
	/** Champs dont la valeur est une liste separee par des virgules. */
	private static final Set<String> MULTI_VALUED_FIELDS = Set.of("auteurs", "mots_cles");

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

	@Column(name = "preuve", columnDefinition = "text")
	private String evidence;

	@Column(name = "signaux", columnDefinition = "text")
	private String signals;

	@Column(name = "segments", columnDefinition = "text")
	private String segments;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MetadataSuggestionSource source = MetadataSuggestionSource.LLM;

	@Column(name = "url_source", columnDefinition = "text")
	private String sourceUrl;

	@Column(name = "distance_edition", precision = 4, scale = 3)
	private BigDecimal editDistance;

	protected MetadataSuggestionEntity() {
	}

	public MetadataSuggestionEntity(EnrichmentEntity enrichment, String champ, String suggestedValue, BigDecimal confidenceScore) {
		this.enrichment = enrichment;
		this.champ = champ;
		this.suggestedValue = suggestedValue;
		this.confidenceScore = confidenceScore;
	}

	public MetadataSuggestionEntity(
			EnrichmentEntity enrichment,
			String champ,
			String suggestedValue,
			BigDecimal confidenceScore,
			String evidence,
			String signals,
			String segments,
			MetadataSuggestionSource source,
			String sourceUrl
	) {
		this(enrichment, champ, suggestedValue, confidenceScore);
		this.evidence = evidence;
		this.signals = signals;
		this.segments = segments;
		this.source = source == null ? MetadataSuggestionSource.LLM : source;
		this.sourceUrl = sourceUrl;
	}

	/**
	 * Compare la valeur publiee a la valeur suggeree et conserve la trace de l'arbitrage humain.
	 * C'est cette trace qui permet de mesurer le taux d'acceptation et l'ampleur des corrections.
	 */
	public void recordDecision(String publishedValue) {
		String suggested = comparable(suggestedValue);
		String published = comparable(publishedValue);
		this.finalValue = publishedValue;
		if (suggested.equals(published)) {
			this.decision = SuggestionDecision.ACCEPTE;
			this.editDistance = BigDecimal.ZERO.setScale(3);
			return;
		}
		this.decision = published.isEmpty() ? SuggestionDecision.VIDE : SuggestionDecision.MODIFIE;
		this.editDistance = BigDecimal.valueOf(EditDistance.normalized(suggested, published))
				.setScale(3, RoundingMode.HALF_UP);
	}

	/**
	 * Met la valeur sous une forme comparable. Pour les champs qui portent une liste,
	 * les elements sont tries : reordonner des auteurs ou des mots-cles n'est pas une correction.
	 */
	private String comparable(String value) {
		String normalized = EditDistance.normalize(value);
		if (!MULTI_VALUED_FIELDS.contains(champ) || normalized.isEmpty()) {
			return normalized;
		}
		return Arrays.stream(normalized.split(","))
				.map(String::trim)
				.filter(part -> !part.isEmpty())
				.sorted()
				.collect(Collectors.joining(", "));
	}

	/** Les metadonnees ont ete rejetees : aucune valeur n'a ete publiee. */
	public void markRejected() {
		this.decision = SuggestionDecision.REJETE;
		this.finalValue = null;
		this.editDistance = null;
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

	public String getEvidence() {
		return evidence;
	}

	public String getSignals() {
		return signals;
	}

	public String getSegments() {
		return segments;
	}

	public MetadataSuggestionSource getSource() {
		return source;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public BigDecimal getEditDistance() {
		return editDistance;
	}
}

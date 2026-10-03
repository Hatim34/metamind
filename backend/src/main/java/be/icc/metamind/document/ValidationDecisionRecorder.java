package be.icc.metamind.document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Confronte chaque valeur publiee par le bibliothecaire a la valeur proposee par le LLM
 * et enregistre l'arbitrage dans suggestions_metadonnees.
 *
 * Sans cette trace, aucun indicateur de fiabilite n'est calculable : c'est elle qui alimente
 * le taux d'acceptation par champ, le taux de correction et la calibration du score de confiance.
 */
@Service
public class ValidationDecisionRecorder {
	private final EnrichmentRepository enrichmentRepository;
	private final MetadataSuggestionRepository suggestionRepository;

	public ValidationDecisionRecorder(
			EnrichmentRepository enrichmentRepository,
			MetadataSuggestionRepository suggestionRepository
	) {
		this.enrichmentRepository = enrichmentRepository;
		this.suggestionRepository = suggestionRepository;
	}

	/**
	 * Enregistre la decision pour chaque champ effectivement soumis a la validation humaine.
	 * Les champs absents de {@code publishedValues} ne recoivent aucune decision : le bibliothecaire
	 * ne les a pas arbitres, et inventer une decision faussait la mesure.
	 */
	public void recordValidation(DocumentEntity document, Map<String, String> publishedValues) {
		for (MetadataSuggestionEntity suggestion : suggestionsOf(document)) {
			if (!publishedValues.containsKey(suggestion.getChamp())) {
				continue;
			}
			suggestion.recordDecision(publishedValues.get(suggestion.getChamp()));
			suggestionRepository.save(suggestion);
		}
	}

	/** Le document a ete rejete : toutes les suggestions du dernier enrichissement sont marquees REJETE. */
	public void recordRejection(DocumentEntity document) {
		for (MetadataSuggestionEntity suggestion : suggestionsOf(document)) {
			suggestion.markRejected();
			suggestionRepository.save(suggestion);
		}
	}

	private List<MetadataSuggestionEntity> suggestionsOf(DocumentEntity document) {
		return latestCompletedEnrichment(document)
				.map(enrichment -> suggestionRepository.findByEnrichment_IdOrderByIdAsc(enrichment.getId()))
				.orElseGet(List::of);
	}

	private Optional<EnrichmentEntity> latestCompletedEnrichment(DocumentEntity document) {
		return enrichmentRepository.findFirstByDocument_IdAndStatusOrderByIdDesc(
				document.getId(), EnrichmentStatus.TERMINE);
	}
}

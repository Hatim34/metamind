package be.icc.metamind.statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import be.icc.metamind.document.MetadataSuggestionEntity;
import be.icc.metamind.document.MetadataSuggestionRepository;
import be.icc.metamind.document.SuggestionDecision;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforme les arbitrages enregistres a la validation en indicateurs de fiabilite.
 * Aucune valeur n'est estimee : tout est calcule sur des decisions humaines reelles.
 */
@Service
public class ExtractionQualityService {
	/** Seuils d'affichage du score de confiance : rouge, orange, vert. */
	private static final List<Threshold> CALIBRATION_THRESHOLDS = List.of(
			new Threshold("0.00-0.60", BigDecimal.ZERO, new BigDecimal("0.60")),
			new Threshold("0.60-0.85", new BigDecimal("0.60"), new BigDecimal("0.85")),
			new Threshold("0.85-1.00", new BigDecimal("0.85"), new BigDecimal("1.01"))
	);

	private final MetadataSuggestionRepository suggestionRepository;

	public ExtractionQualityService(MetadataSuggestionRepository suggestionRepository) {
		this.suggestionRepository = suggestionRepository;
	}

	@Transactional(readOnly = true)
	public ExtractionQualityResponse getQuality(UserEntity currentUser) {
		boolean admin = currentUser.getRole() == UserRole.ADMIN;
		Long institutionId = admin ? null : currentUser.getInstitution().getId();
		List<MetadataSuggestionEntity> arbitrated = suggestionRepository.findArbitrated(institutionId);
		String scope = admin ? "GLOBAL" : currentUser.getInstitution().getName();

		return new ExtractionQualityResponse(
				scope,
				arbitrated.size(),
				rate(count(arbitrated, SuggestionDecision.ACCEPTE), arbitrated.size()),
				byField(arbitrated),
				calibration(arbitrated)
		);
	}

	private List<ExtractionQualityResponse.FieldQuality> byField(List<MetadataSuggestionEntity> arbitrated) {
		Map<String, List<MetadataSuggestionEntity>> grouped = new LinkedHashMap<>();
		for (MetadataSuggestionEntity suggestion : arbitrated) {
			grouped.computeIfAbsent(suggestion.getChamp(), key -> new ArrayList<>()).add(suggestion);
		}
		return grouped.entrySet().stream()
				.map(entry -> fieldQuality(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparing(ExtractionQualityResponse.FieldQuality::field))
				.toList();
	}

	private ExtractionQualityResponse.FieldQuality fieldQuality(String field, List<MetadataSuggestionEntity> suggestions) {
		long accepted = count(suggestions, SuggestionDecision.ACCEPTE);
		return new ExtractionQualityResponse.FieldQuality(
				field,
				suggestions.size(),
				accepted,
				count(suggestions, SuggestionDecision.MODIFIE),
				count(suggestions, SuggestionDecision.VIDE),
				count(suggestions, SuggestionDecision.REJETE),
				rate(accepted, suggestions.size()),
				averageEditDistance(suggestions)
		);
	}

	private List<ExtractionQualityResponse.CalibrationBucket> calibration(List<MetadataSuggestionEntity> arbitrated) {
		return CALIBRATION_THRESHOLDS.stream()
				.map(threshold -> {
					List<MetadataSuggestionEntity> inRange = arbitrated.stream()
							.filter(suggestion -> threshold.contains(suggestion.getConfidenceScore()))
							.toList();
					long accepted = count(inRange, SuggestionDecision.ACCEPTE);
					return new ExtractionQualityResponse.CalibrationBucket(
							threshold.label(),
							inRange.size(),
							accepted,
							rate(accepted, inRange.size())
					);
				})
				.toList();
	}

	private long count(List<MetadataSuggestionEntity> suggestions, SuggestionDecision decision) {
		return suggestions.stream().filter(suggestion -> suggestion.getDecision() == decision).count();
	}

	private double averageEditDistance(List<MetadataSuggestionEntity> suggestions) {
		double[] distances = suggestions.stream()
				.map(MetadataSuggestionEntity::getEditDistance)
				.filter(Objects::nonNull)
				.mapToDouble(BigDecimal::doubleValue)
				.toArray();
		if (distances.length == 0) {
			return 0.0;
		}
		double sum = 0.0;
		for (double distance : distances) {
			sum += distance;
		}
		return round(sum / distances.length);
	}

	private double rate(long count, long total) {
		if (total == 0) {
			return 0.0;
		}
		return Math.round((count * 10000.0) / total) / 100.0;
	}

	private double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	private record Threshold(String label, BigDecimal minimumInclusive, BigDecimal maximumExclusive) {
		boolean contains(BigDecimal score) {
			return score != null
					&& score.compareTo(minimumInclusive) >= 0
					&& score.compareTo(maximumExclusive) < 0;
		}
	}
}

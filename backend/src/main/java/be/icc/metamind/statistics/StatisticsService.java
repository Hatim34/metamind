package be.icc.metamind.statistics;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.institution.InstitutionRepository;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final InstitutionRepository institutionRepository;

	public StatisticsService(DocumentRepository documentRepository, MetadataRepository metadataRepository, InstitutionRepository institutionRepository) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.institutionRepository = institutionRepository;
	}

	@Transactional(readOnly = true)
	public StatisticsResponse getStatistics(UserEntity currentUser) {
		return getStatistics(currentUser, null, null);
	}

	@Transactional(readOnly = true)
	public StatisticsResponse getStatistics(UserEntity currentUser, LocalDate startDate, LocalDate endDate) {
		List<DocumentEntity> documents = scopedDocuments(currentUser, startDate, endDate);
		long total = documents.size();
		long published = countStatus(documents, DocumentStatus.PUBLIE);
		long pendingValidation = countStatus(documents, DocumentStatus.A_VALIDER);
		long publicDocuments = countVisibility(documents, DocumentVisibility.PUBLIC);
		long institutionOnlyDocuments = countVisibility(documents, DocumentVisibility.INSTITUTION);
		long rejected = countStatus(documents, DocumentStatus.SUPPRIME);
		int creditBalance = creditBalance(currentUser);
		String scope = currentUser.getRole() == UserRole.ADMIN ? "GLOBAL" : currentUser.getInstitution().getName();
		return new StatisticsResponse(
				scope,
				total,
				published,
				pendingValidation,
				publicDocuments,
				institutionOnlyDocuments,
				creditBalance,
				rate(published, total),
				rate(rejected, total),
				averageProcessingHours(documents),
				distributionByDocumentType(documents),
				distributionByClassification(documents)
		);
	}

	private List<DocumentEntity> scopedDocuments(UserEntity currentUser, LocalDate startDate, LocalDate endDate) {
		return documentRepository.findAll().stream()
				.filter(document -> currentUser.getRole() == UserRole.ADMIN
						|| Objects.equals(document.getInstitution().getId(), currentUser.getInstitution().getId()))
				.filter(document -> matchesPeriod(document, startDate, endDate))
				.toList();
	}

	private boolean matchesPeriod(DocumentEntity document, LocalDate startDate, LocalDate endDate) {
		if (startDate == null && endDate == null) {
			return true;
		}
		LocalDate publicationDate = metadataRepository.findByDocumentId(document.getId())
				.map(MetadataEntity::getPublicationDate)
				.orElse(null);
		if (publicationDate == null) {
			return false;
		}
		return (startDate == null || !publicationDate.isBefore(startDate))
				&& (endDate == null || !publicationDate.isAfter(endDate));
	}

	private long countStatus(List<DocumentEntity> documents, DocumentStatus status) {
		return documents.stream().filter(document -> document.getStatus() == status).count();
	}

	private long countVisibility(List<DocumentEntity> documents, DocumentVisibility visibility) {
		return documents.stream().filter(document -> document.getVisibility() == visibility).count();
	}

	private int creditBalance(UserEntity currentUser) {
		if (currentUser.getRole() == UserRole.ADMIN) {
			return institutionRepository.findAll().stream()
					.mapToInt(institution -> institution.getCreditBalance())
					.sum();
		}
		return currentUser.getInstitution().getCreditBalance();
	}

	private double rate(long count, long total) {
		if (total == 0) {
			return 0.0;
		}
		return Math.round((count * 10000.0) / total) / 100.0;
	}

	private double averageProcessingHours(List<DocumentEntity> documents) {
		List<Long> durations = documents.stream()
				.map(document -> metadataRepository.findByDocumentId(document.getId()).orElse(null))
				.filter(Objects::nonNull)
				.filter(metadata -> metadata.getGeneratedAt() != null && metadata.getValidatedAt() != null)
				.map(metadata -> Duration.between(metadata.getGeneratedAt(), metadata.getValidatedAt()).toMinutes())
				.toList();
		if (durations.isEmpty()) {
			return 0.0;
		}
		double averageMinutes = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
		return Math.round((averageMinutes / 60.0) * 100.0) / 100.0;
	}

	private Map<String, Long> distributionByDocumentType(List<DocumentEntity> documents) {
		return orderedDistribution(documents.stream()
				.map(document -> metadataRepository.findByDocumentId(document.getId()).orElse(null))
				.filter(Objects::nonNull)
				.map(metadata -> metadata.getDocumentType() == null ? "Non renseigne" : metadata.getDocumentType().getLibelle())
				.collect(Collectors.groupingBy(value -> value, TreeMap::new, Collectors.counting())));
	}

	private Map<String, Long> distributionByClassification(List<DocumentEntity> documents) {
		return orderedDistribution(documents.stream()
				.map(document -> metadataRepository.findByDocumentId(document.getId()).orElse(null))
				.filter(Objects::nonNull)
				.map(metadata -> metadata.getClassification() == null || metadata.getClassification().isBlank() ? "Non renseigne" : metadata.getClassification())
				.collect(Collectors.groupingBy(value -> value, TreeMap::new, Collectors.counting())));
	}

	private Map<String, Long> orderedDistribution(Map<String, Long> values) {
		return values.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
	}
}

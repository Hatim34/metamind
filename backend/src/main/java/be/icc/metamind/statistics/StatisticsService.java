package be.icc.metamind.statistics;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataStatus;
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
		// Les metadonnees sont chargees une seule fois : les agregats lisaient la base par document.
		Map<Long, MetadataEntity> metadataByDocument = metadataByDocument(currentUser);
		List<DocumentEntity> documents = scopedDocuments(currentUser, startDate, endDate, metadataByDocument);
		long total = documents.size();
		long published = countStatus(documents, DocumentStatus.PUBLIE);
		long pendingValidation = countStatus(documents, DocumentStatus.A_VALIDER);
		long publicDocuments = countVisibility(documents, DocumentVisibility.PUBLIC);
		long institutionOnlyDocuments = countVisibility(documents, DocumentVisibility.INSTITUTION);
		long rejected = countRejectedMetadata(documents, metadataByDocument);
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
				averageProcessingHours(documents, metadataByDocument),
				distributionByDocumentType(documents, metadataByDocument),
				distributionByClassification(documents, metadataByDocument)
		);
	}

	private Map<Long, MetadataEntity> metadataByDocument(UserEntity currentUser) {
		Long institutionId = currentUser.getRole() == UserRole.ADMIN
				? null
				: currentUser.getInstitution().getId();
		Map<Long, MetadataEntity> byDocument = new HashMap<>();
		for (MetadataEntity metadata : metadataRepository.findForInstitution(institutionId)) {
			byDocument.putIfAbsent(metadata.getDocument().getId(), metadata);
		}
		return byDocument;
	}

	private List<DocumentEntity> scopedDocuments(
			UserEntity currentUser,
			LocalDate startDate,
			LocalDate endDate,
			Map<Long, MetadataEntity> metadataByDocument
	) {
		return documentRepository.findAll().stream()
				.filter(document -> currentUser.getRole() == UserRole.ADMIN
						|| Objects.equals(document.getInstitution().getId(), currentUser.getInstitution().getId()))
				.filter(document -> matchesPeriod(metadataByDocument.get(document.getId()), startDate, endDate))
				.toList();
	}

	private boolean matchesPeriod(MetadataEntity metadata, LocalDate startDate, LocalDate endDate) {
		if (startDate == null && endDate == null) {
			return true;
		}
		LocalDate publicationDate = metadata == null ? null : metadata.getPublicationDate();
		if (publicationDate == null) {
			return false;
		}
		return (startDate == null || !publicationDate.isBefore(startDate))
				&& (endDate == null || !publicationDate.isAfter(endDate));
	}

	private long countStatus(List<DocumentEntity> documents, DocumentStatus status) {
		return documents.stream().filter(document -> document.getStatus() == status).count();
	}

	private long countRejectedMetadata(List<DocumentEntity> documents, Map<Long, MetadataEntity> metadataByDocument) {
		return metadataOf(documents, metadataByDocument)
				.filter(metadata -> metadata.getStatus() == MetadataStatus.REJETE)
				.count();
	}

	/** Metadonnees des documents du perimetre, sans requete supplementaire. */
	private Stream<MetadataEntity> metadataOf(List<DocumentEntity> documents, Map<Long, MetadataEntity> metadataByDocument) {
		return documents.stream()
				.map(document -> metadataByDocument.get(document.getId()))
				.filter(Objects::nonNull);
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

	private double averageProcessingHours(List<DocumentEntity> documents, Map<Long, MetadataEntity> metadataByDocument) {
		List<Long> durations = metadataOf(documents, metadataByDocument)
				.filter(metadata -> metadata.getGeneratedAt() != null && metadata.getValidatedAt() != null)
				.map(metadata -> Duration.between(metadata.getGeneratedAt(), metadata.getValidatedAt()).toMinutes())
				.toList();
		if (durations.isEmpty()) {
			return 0.0;
		}
		double averageMinutes = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
		return Math.round((averageMinutes / 60.0) * 100.0) / 100.0;
	}

	private Map<String, Long> distributionByDocumentType(List<DocumentEntity> documents, Map<Long, MetadataEntity> metadataByDocument) {
		return orderedDistribution(metadataOf(documents, metadataByDocument)
				.map(metadata -> metadata.getDocumentType() == null ? "Non renseigne" : metadata.getDocumentType().getLibelle())
				.collect(Collectors.groupingBy(value -> value, TreeMap::new, Collectors.counting())));
	}

	private Map<String, Long> distributionByClassification(List<DocumentEntity> documents, Map<Long, MetadataEntity> metadataByDocument) {
		return orderedDistribution(metadataOf(documents, metadataByDocument)
				.map(metadata -> metadata.getClassification() == null || metadata.getClassification().isBlank() ? "Non renseigne" : metadata.getClassification())
				.collect(Collectors.groupingBy(value -> value, TreeMap::new, Collectors.counting())));
	}

	private Map<String, Long> orderedDistribution(Map<String, Long> values) {
		return values.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
	}
}

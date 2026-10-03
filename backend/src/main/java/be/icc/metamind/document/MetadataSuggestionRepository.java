package be.icc.metamind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetadataSuggestionRepository extends JpaRepository<MetadataSuggestionEntity, Long> {
	List<MetadataSuggestionEntity> findByEnrichment_IdOrderByIdAsc(Long enrichmentId);

	/**
	 * Suggestions reellement arbitrees par un bibliothecaire, cloisonnees par institution.
	 * Un institutionId null donne la vue globale reservee a l'administrateur.
	 */
	@Query("""
			select suggestion
			from MetadataSuggestionEntity suggestion
			where suggestion.decision is not null
			  and (:institutionId is null
			       or suggestion.enrichment.document.institution.id = :institutionId)
			""")
	List<MetadataSuggestionEntity> findArbitrated(@Param("institutionId") Long institutionId);
}

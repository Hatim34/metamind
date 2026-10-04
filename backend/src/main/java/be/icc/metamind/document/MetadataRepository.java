package be.icc.metamind.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetadataRepository extends JpaRepository<MetadataEntity, Long> {
	Optional<MetadataEntity> findByDocumentId(Long documentId);

	boolean existsByTitreIgnoreCase(String titre);

	/** Un DOI identifie une seule publication : sert a refuser un doublon avant d'ecrire. */
	boolean existsByDoiIgnoreCaseAndDocument_IdNot(String doi, Long documentId);

	/**
	 * Charge en une requete les metadonnees d'une institution, avec leurs references.
	 * Evite une requete par document lors du calcul des statistiques.
	 * Un institutionId null donne la vue globale reservee a l'administrateur.
	 */
	@Query("""
			select metadata
			from MetadataEntity metadata
			left join fetch metadata.documentType
			left join fetch metadata.language
			where :institutionId is null
			   or metadata.document.institution.id = :institutionId
			""")
	List<MetadataEntity> findForInstitution(@Param("institutionId") Long institutionId);
}

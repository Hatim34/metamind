package be.icc.metamind.document;

import java.util.List;

import be.icc.metamind.institution.InstitutionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
	/**
	 * Recherche sur l'index de recherche du document, jamais sur le texte integral.
	 *
	 * search_vector reprend titre, resume, classification et mots-cles : environ 1 ko
	 * par document, contre 75 ko en moyenne pour le texte extrait. Balayer le texte
	 * integral imposait un lower() puis un LIKE sur une douzaine de megaoctets a chaque
	 * recherche, soit une quarantaine de secondes de reponse.
	 */
	@Query("""
			select distinct new be.icc.metamind.document.DocumentSummary(
				d.id, d.fileName, d.filePath, d.coverImagePath, d.status, d.visibility, i.id, i.name)
			from DocumentEntity d
			join d.institution i
			left join MetadataEntity m on m.document = d
			left join DocumentAuthorEntity da on da.document = d
			left join da.author a
			left join DocumentKeywordEntity dk on dk.document = d
			left join dk.keyword k
			where lower(coalesce(m.titre, '')) like lower(concat('%', :search, '%'))
			   or lower(coalesce(a.fullName, '')) like lower(concat('%', :search, '%'))
			   or lower(coalesce(k.libelle, '')) like lower(concat('%', :search, '%'))
			   or lower(coalesce(d.searchVector, '')) like lower(concat('%', :search, '%'))
			""")
	List<DocumentSummary> search(@Param("search") String search);

	/** Tous les documents, sans le texte extrait. Reserve a la vue globale de l'administrateur. */
	@Query("""
			select new be.icc.metamind.document.DocumentSummary(
				d.id, d.fileName, d.filePath, d.coverImagePath, d.status, d.visibility, i.id, i.name)
			from DocumentEntity d
			join d.institution i
			""")
	List<DocumentSummary> findSummaries();

	/** Documents d'une institution, sans le texte extrait. */
	@Query("""
			select new be.icc.metamind.document.DocumentSummary(
				d.id, d.fileName, d.filePath, d.coverImagePath, d.status, d.visibility, i.id, i.name)
			from DocumentEntity d
			join d.institution i
			where i.id = :institutionId
			""")
	List<DocumentSummary> findSummariesByInstitution(@Param("institutionId") Long institutionId);

	/** Documents du catalogue public : publies et en acces public. */
	@Query("""
			select new be.icc.metamind.document.DocumentSummary(
				d.id, d.fileName, d.filePath, d.coverImagePath, d.status, d.visibility, i.id, i.name)
			from DocumentEntity d
			join d.institution i
			where d.status = be.icc.metamind.document.DocumentStatus.PUBLIE
			  and d.visibility = be.icc.metamind.document.DocumentVisibility.PUBLIC
			""")
	List<DocumentSummary> findPublishedSummaries();

	/** Documents visibles par un utilisateur connecte : ceux de son institution, plus les publics. */
	@Query("""
			select new be.icc.metamind.document.DocumentSummary(
				d.id, d.fileName, d.filePath, d.coverImagePath, d.status, d.visibility, i.id, i.name)
			from DocumentEntity d
			join d.institution i
			where i.id = :institutionId
			   or (d.status = be.icc.metamind.document.DocumentStatus.PUBLIE
			       and d.visibility = be.icc.metamind.document.DocumentVisibility.PUBLIC)
			""")
	List<DocumentSummary> findSummariesVisibleForInstitution(@Param("institutionId") Long institutionId);

	/** Documents importes sous ce nom de fichier, pour leur rattacher un fichier perdu. */
	@Query("select d.id from DocumentEntity d where d.fileName = :fileName")
	List<Long> findIdsByFileName(@Param("fileName") String fileName);

	long countByInstitution(InstitutionEntity institution);

	long countByStatus(DocumentStatus status);

	long countByInstitutionAndStatus(InstitutionEntity institution, DocumentStatus status);

	long countByVisibility(DocumentVisibility visibility);

	long countByInstitutionAndVisibility(InstitutionEntity institution, DocumentVisibility visibility);
}

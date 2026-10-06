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
			select distinct d
			from DocumentEntity d
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
	List<DocumentEntity> search(@Param("search") String search);

	/**
	 * Documents visibles par le catalogue public : publies et en acces public.
	 *
	 * Le filtrage se fait en base. Charger toute la table puis filtrer en memoire
	 * ramenait aussi le texte integral de chaque document, soit une douzaine de
	 * megaoctets a chaque affichage du catalogue.
	 */
	@Query("""
			select d from DocumentEntity d
			where d.status = be.icc.metamind.document.DocumentStatus.PUBLIE
			  and d.visibility = be.icc.metamind.document.DocumentVisibility.PUBLIC
			""")
	List<DocumentEntity> findPubliclyVisible();

	/** Documents visibles par un utilisateur connecte : ceux de son institution, plus les publics. */
	@Query("""
			select d from DocumentEntity d
			where d.institution.id = :institutionId
			   or (d.status = be.icc.metamind.document.DocumentStatus.PUBLIE
			       and d.visibility = be.icc.metamind.document.DocumentVisibility.PUBLIC)
			""")
	List<DocumentEntity> findVisibleForInstitution(@Param("institutionId") Long institutionId);

	long countByInstitution(InstitutionEntity institution);

	long countByStatus(DocumentStatus status);

	long countByInstitutionAndStatus(InstitutionEntity institution, DocumentStatus status);

	long countByVisibility(DocumentVisibility visibility);

	long countByInstitutionAndVisibility(InstitutionEntity institution, DocumentVisibility visibility);
}

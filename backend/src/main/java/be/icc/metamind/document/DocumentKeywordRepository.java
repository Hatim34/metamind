package be.icc.metamind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentKeywordRepository extends JpaRepository<DocumentKeywordEntity, DocumentKeywordId> {
	List<DocumentKeywordEntity> findByDocument_Id(Long documentId);

	/** Mots-cles de plusieurs documents en une requete, le mot-cle etant charge avec le lien. */
	@Query("""
			select dk from DocumentKeywordEntity dk
			join fetch dk.keyword
			where dk.document.id in :documentIds
			""")
	List<DocumentKeywordEntity> findForDocuments(@Param("documentIds") List<Long> documentIds);

	void deleteByDocument_Id(Long documentId);
}

package be.icc.metamind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentAuthorRepository extends JpaRepository<DocumentAuthorEntity, DocumentAuthorId> {
	List<DocumentAuthorEntity> findByDocument_IdOrderByAuthorOrderAsc(Long documentId);

	/** Auteurs de plusieurs documents en une requete, l'auteur etant charge avec le lien. */
	@Query("""
			select da from DocumentAuthorEntity da
			join fetch da.author
			where da.document.id in :documentIds
			order by da.authorOrder asc
			""")
	List<DocumentAuthorEntity> findForDocuments(@Param("documentIds") List<Long> documentIds);

	void deleteByDocument_Id(Long documentId);
}

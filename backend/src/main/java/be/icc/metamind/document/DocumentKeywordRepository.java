package be.icc.metamind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentKeywordRepository extends JpaRepository<DocumentKeywordEntity, DocumentKeywordId> {
	List<DocumentKeywordEntity> findByDocument_Id(Long documentId);
}

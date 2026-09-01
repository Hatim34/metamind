package be.icc.metamind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAuthorRepository extends JpaRepository<DocumentAuthorEntity, DocumentAuthorId> {
	List<DocumentAuthorEntity> findByDocument_IdOrderByAuthorOrderAsc(Long documentId);

	void deleteByDocument_Id(Long documentId);
}

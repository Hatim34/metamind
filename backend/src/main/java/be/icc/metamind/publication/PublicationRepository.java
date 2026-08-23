package be.icc.metamind.publication;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<PublicationEntity, Long> {
	List<PublicationEntity> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrKeywordsTextContainingIgnoreCase(String title, String author, String keywordsText);
}

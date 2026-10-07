package be.icc.metamind.publication;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationTranslationRepository extends JpaRepository<PublicationTranslationEntity, Long> {
	Optional<PublicationTranslationEntity> findByDocumentIdAndTargetLanguage(Long documentId, String targetLanguage);

	List<PublicationTranslationEntity> findByTargetLanguageAndDocument_IdIn(String targetLanguage, Collection<Long> documentIds);
}

package be.icc.metamind.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadataRepository extends JpaRepository<MetadataEntity, Long> {
	Optional<MetadataEntity> findByDocumentId(Long documentId);
}

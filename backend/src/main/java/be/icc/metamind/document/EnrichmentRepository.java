package be.icc.metamind.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrichmentRepository extends JpaRepository<EnrichmentEntity, Long> {
	/** Dernier enrichissement reussi d'un document : c'est lui qui porte les suggestions a arbitrer. */
	Optional<EnrichmentEntity> findFirstByDocument_IdAndStatusOrderByIdDesc(Long documentId, EnrichmentStatus status);
}

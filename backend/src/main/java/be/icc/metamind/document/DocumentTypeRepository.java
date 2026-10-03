package be.icc.metamind.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTypeRepository extends JpaRepository<DocumentTypeEntity, Long> {
	Optional<DocumentTypeEntity> findByCodeIgnoreCase(String code);
}

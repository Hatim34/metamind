package be.icc.metamind.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<KeywordEntity, Long> {
	Optional<KeywordEntity> findByLibelleIgnoreCase(String libelle);
}

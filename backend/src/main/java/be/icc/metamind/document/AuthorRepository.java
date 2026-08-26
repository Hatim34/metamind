package be.icc.metamind.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {
	Optional<AuthorEntity> findByFullNameIgnoreCase(String fullName);
}

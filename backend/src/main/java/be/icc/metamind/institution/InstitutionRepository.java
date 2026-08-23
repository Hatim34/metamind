package be.icc.metamind.institution;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long> {
	Optional<InstitutionEntity> findByCodeIgnoreCase(String code);

	boolean existsByEmailDomainIgnoreCase(String emailDomain);
}

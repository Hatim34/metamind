package be.icc.metamind.institution;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long> {
	Optional<InstitutionEntity> findByCodeIgnoreCase(String code);

	Optional<InstitutionEntity> findByNameIgnoreCase(String name);

	boolean existsByEmailDomainIgnoreCase(String emailDomain);
}

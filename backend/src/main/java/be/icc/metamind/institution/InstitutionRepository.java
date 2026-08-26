package be.icc.metamind.institution;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long> {
	default Optional<InstitutionEntity> findByCodeIgnoreCase(String code) {
		String expected = code == null ? "" : code.trim().toUpperCase();
		return findAll().stream()
				.filter(institution -> institution.getCode().equalsIgnoreCase(expected))
				.findFirst();
	}

	Optional<InstitutionEntity> findByNameIgnoreCase(String name);

	boolean existsByEmailDomainIgnoreCase(String emailDomain);

	Optional<InstitutionEntity> findByEmailDomainIgnoreCase(String emailDomain);
}

package be.icc.metamind.institution;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Modifying(flushAutomatically = true)
	@Query("update InstitutionEntity i set i.creditBalance = i.creditBalance - 1 where i.id = :id and i.creditBalance > 0")
	int reserveCredit(@Param("id") long institutionId);

	@Modifying(flushAutomatically = true)
	@Query("update InstitutionEntity i set i.creditBalance = i.creditBalance + 1 where i.id = :id")
	int refundCredit(@Param("id") long institutionId);
}

package be.icc.metamind.credit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditMovementRepository extends JpaRepository<CreditMovementEntity, Long> {
	List<CreditMovementEntity> findByInstitutionIdOrderByCreatedAtDesc(Long institutionId);
}

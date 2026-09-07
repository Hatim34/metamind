package be.icc.metamind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
	List<AuditLogEntity> findAllByOrderByCreatedAtDesc();

	List<AuditLogEntity> findByActionAndEntityTypeAndEntityIdOrderByCreatedAtDesc(String action, String entityType, Long entityId);
}

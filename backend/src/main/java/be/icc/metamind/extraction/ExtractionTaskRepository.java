package be.icc.metamind.extraction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionTaskRepository extends JpaRepository<ExtractionTaskEntity, Long> {
	List<ExtractionTaskEntity> findByBatchIdOrderByCreatedAtAsc(String batchId);
}

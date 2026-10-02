package be.icc.metamind.credit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeProcessedEventRepository extends JpaRepository<StripeProcessedEventEntity, Long> {
	boolean existsByEventId(String eventId);
}

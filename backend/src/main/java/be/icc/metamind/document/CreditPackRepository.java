package be.icc.metamind.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditPackRepository extends JpaRepository<CreditPackEntity, Long> {
	Optional<CreditPackEntity> findByPaymentReference(String paymentReference);
}

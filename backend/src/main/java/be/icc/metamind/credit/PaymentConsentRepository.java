package be.icc.metamind.credit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentConsentRepository extends JpaRepository<PaymentConsentEntity, Long> {
}

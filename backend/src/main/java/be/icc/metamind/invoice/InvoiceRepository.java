package be.icc.metamind.invoice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
	Optional<InvoiceEntity> findByPack_Id(Long packId);

	List<InvoiceEntity> findByInstitution_IdOrderByIdDesc(Long institutionId);

	List<InvoiceEntity> findAllByOrderByIdDesc();
}

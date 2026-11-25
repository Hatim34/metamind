package be.icc.metamind.invoice;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounterEntity, Integer> {
	/** Verrouille le compteur de l'annee : deux paiements simultanes ne recoivent jamais le meme numero. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from InvoiceCounterEntity c where c.year = :year")
	Optional<InvoiceCounterEntity> lockYear(@Param("year") int year);
}

package be.icc.metamind.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Dernier numero attribue pour une annee : la numerotation reste continue, sans trou ni doublon. */
@Entity
@Table(name = "compteurs_factures")
public class InvoiceCounterEntity {
	@Id
	@Column(name = "annee")
	private Integer year;

	@Column(name = "dernier_numero", nullable = false)
	private int lastNumber;

	protected InvoiceCounterEntity() {
	}

	public InvoiceCounterEntity(int year) {
		this.year = year;
	}

	public int next() {
		lastNumber++;
		return lastNumber;
	}
}

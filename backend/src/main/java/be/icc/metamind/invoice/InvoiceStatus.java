package be.icc.metamind.invoice;

public enum InvoiceStatus {
	/** L'achat est paye mais les donnees de facturation de l'institution manquent : pas encore de numero. */
	A_COMPLETER,
	/** Facture numerotee, XML Peppol produit. Elle ne change plus. */
	EMISE
}

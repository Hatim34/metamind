package be.icc.metamind.publication;

/** Statut expose par l'API : meme cycle de vie que DocumentStatus. */
public enum PublicationStatus {
	EN_ATTENTE,
	EXTRACTION,
	A_VALIDER,
	REJETE,
	ECHEC,
	PUBLIE,
	SUPPRIME
}

package be.icc.metamind.document;

/**
 * Cycle de vie d'un document (livrable 07, diagramme d'etat-transition).
 *
 * EN_ATTENTE : importe ; texte en cours de lecture, puis pret pour l'analyse par l'IA.
 * EXTRACTION : lecture du fichier ou analyse par l'IA en cours.
 * A_VALIDER : notice proposee par l'IA, a relire par un bibliothecaire.
 * REJETE : notice refusee par le bibliothecaire ; l'analyse peut etre relancee.
 * ECHEC : fichier illisible ; le traitement du fichier peut etre relance.
 * PUBLIE, SUPPRIME : notice validee, document archive (suppression logique).
 */
public enum DocumentStatus {
	EN_ATTENTE,
	EXTRACTION,
	A_VALIDER,
	REJETE,
	ECHEC,
	PUBLIE,
	SUPPRIME
}

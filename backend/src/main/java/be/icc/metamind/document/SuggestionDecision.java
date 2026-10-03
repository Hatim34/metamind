package be.icc.metamind.document;

/**
 * Sort reserve par le bibliothecaire a une valeur proposee par le LLM.
 * Sert de base a la mesure du gain : taux d'acceptation et taux de correction par champ.
 */
public enum SuggestionDecision {
	/** La valeur publiee est identique a la suggestion (apres normalisation). */
	ACCEPTE,
	/** La valeur publiee differe de la suggestion : le bibliothecaire a corrige. */
	MODIFIE,
	/** La suggestion proposait une valeur, le bibliothecaire l'a effacee. */
	VIDE,
	/** Les metadonnees du document ont ete rejetees en bloc. */
	REJETE
}

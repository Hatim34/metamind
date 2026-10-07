package be.icc.metamind.document;

/** Une notice vient d'etre publiee : ses traductions d'affichage peuvent etre preparees. */
public record DocumentPublishedEvent(long documentId) {
}

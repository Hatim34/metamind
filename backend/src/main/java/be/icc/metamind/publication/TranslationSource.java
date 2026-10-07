package be.icc.metamind.publication;

import java.util.List;

/** Donnees source envoyees au fournisseur de traduction. */
public record TranslationSource(String title, String summary, List<String> keywords, String classification) {
}

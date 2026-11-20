package be.icc.metamind.extraction;

import java.util.List;
import java.util.Map;

/**
 * Sortie d'un fournisseur d'extraction.
 *
 * {@code modelConfidences} porte la confiance declaree par le modele pour chaque champ
 * (clefs : titre, auteurs, resume, classification, mots_cles, date_publication, doi).
 * Une entree absente signifie que le fournisseur n'a annonce aucune confiance : le score est
 * alors calcule sur les seules verifications locales, sans valeur de remplacement inventee.
 *
 * {@code publicationDate} est transmise telle que le modele l'a lue (format ISO attendu) et
 * reste une chaine : une date illisible doit etre signalee au bibliothecaire, pas corrigee en silence.
 *
 * {@code maskedData} compte les donnees personnelles masquees avant l'envoi au modele, par categorie.
 */
public record MetadataExtractionData(
		String title,
		String author,
		String summary,
		String classification,
		List<String> keywords,
		String publicationDate,
		String doi,
		Map<String, Double> modelConfidences,
		Map<String, Integer> maskedData
) {
	public MetadataExtractionData(
			String title,
			String author,
			String summary,
			String classification,
			List<String> keywords,
			String publicationDate,
			String doi,
			Map<String, Double> modelConfidences
	) {
		this(title, author, summary, classification, keywords, publicationDate, doi, modelConfidences, Map.of());
	}

	public MetadataExtractionData(
			String title,
			String author,
			String summary,
			String classification,
			List<String> keywords
	) {
		this(title, author, summary, classification, keywords, null, null, Map.of());
	}

	public MetadataExtractionData(
			String title,
			String author,
			String summary,
			String classification,
			List<String> keywords,
			Map<String, Double> modelConfidences
	) {
		this(title, author, summary, classification, keywords, null, null, modelConfidences);
	}

	public MetadataExtractionData withMaskedData(Map<String, Integer> masked) {
		return new MetadataExtractionData(title, author, summary, classification, keywords, publicationDate, doi, modelConfidences,
				masked == null ? Map.of() : masked);
	}

	/** Confiance annoncee par le modele pour ce champ, ou null si le modele n'en a pas donne. */
	public Double modelConfidenceOf(String field) {
		return modelConfidences == null ? null : modelConfidences.get(field);
	}
}

package be.icc.metamind.extraction;

import be.icc.metamind.document.DocumentEntity;

public interface MetadataExtractionProvider {
	MetadataExtractionData extract(DocumentEntity document);

	default String modelName() {
		return "local";
	}

	/** Version du prompt reellement utilisee, conservee dans enrichissements pour la tracabilite. */
	default String promptVersion() {
		return "local-v1";
	}
}

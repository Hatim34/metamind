package be.icc.metamind.extraction;

import be.icc.metamind.document.DocumentEntity;

public interface MetadataExtractionProvider {
	MetadataExtractionData extract(DocumentEntity document);

	default String modelName() {
		return "local";
	}
}

package be.icc.metamind.extraction;

import be.icc.metamind.publication.PublicationEntity;

public interface MetadataExtractionProvider {
	MetadataExtractionData extract(PublicationEntity publication);
}

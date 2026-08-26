package be.icc.metamind.extraction;

import java.util.List;

import be.icc.metamind.publication.PublicationEntity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "local", matchIfMissing = true)
public class LocalMetadataExtractionProvider implements MetadataExtractionProvider {
	@Override
	public MetadataExtractionData extract(PublicationEntity publication) {
		return new MetadataExtractionData(
				publication.getTitle(),
				publication.getAuthor(),
				suggestedKeywords(publication)
		);
	}

	private List<String> suggestedKeywords(PublicationEntity publication) {
		String title = publication.getTitle().toLowerCase();
		if (title.contains("metadonnees")) {
			return List.of("metadonnees", "Dublin Core", "catalogage");
		}
		if (title.contains("multilingue")) {
			return List.of("multilingue", "indexation", "recherche");
		}
		return List.of("publication", "validation", "bibliotheque");
	}
}

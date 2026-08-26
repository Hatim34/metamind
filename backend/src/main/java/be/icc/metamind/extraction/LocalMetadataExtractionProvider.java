package be.icc.metamind.extraction;

import java.util.List;

import be.icc.metamind.document.DocumentEntity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "local", matchIfMissing = true)
public class LocalMetadataExtractionProvider implements MetadataExtractionProvider {
	@Override
	public MetadataExtractionData extract(DocumentEntity document) {
		return new MetadataExtractionData(
				document.getFileName().replace(".txt", ""),
				document.getImportedBy() == null ? "Auteur non renseigne" : document.getImportedBy().getFirstName() + " " + document.getImportedBy().getLastName(),
				suggestedKeywords(document)
		);
	}

	private List<String> suggestedKeywords(DocumentEntity document) {
		String title = (document.getFileName() + " " + document.getExtractedText()).toLowerCase();
		if (title.contains("metadonnees")) {
			return List.of("metadonnees", "Dublin Core", "catalogage");
		}
		if (title.contains("multilingue")) {
			return List.of("multilingue", "indexation", "recherche");
		}
		return List.of("publication", "validation", "bibliotheque");
	}
}

package be.icc.metamind.extraction;

import java.util.List;
import java.util.Locale;

import be.icc.metamind.document.DocumentEntity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "metamind.llm.provider", havingValue = "local", matchIfMissing = true)
public class LocalMetadataExtractionProvider implements MetadataExtractionProvider {
	@Override
	public MetadataExtractionData extract(DocumentEntity document) {
		String text = text(document);
		List<String> keywords = suggestedKeywords(document);
		return new MetadataExtractionData(
				title(document),
				document.getImportedBy() == null ? "Auteur non renseigne" : document.getImportedBy().getFirstName() + " " + document.getImportedBy().getLastName(),
				summary(text),
				classification(keywords),
				keywords
		);
	}

	private List<String> suggestedKeywords(DocumentEntity document) {
		String text = (document.getFileName() + " " + document.getExtractedText()).toLowerCase(Locale.ROOT);
		if (text.contains("metadonnees")) {
			return List.of("metadonnees", "Dublin Core", "catalogage");
		}
		if (text.contains("multilingue")) {
			return List.of("multilingue", "indexation", "recherche");
		}
		return List.of("publication", "validation", "bibliotheque");
	}

	private String title(DocumentEntity document) {
		String fileName = document.getFileName();
		if (fileName == null || fileName.isBlank()) {
			return "Document importe";
		}
		int index = fileName.lastIndexOf('.');
		String withoutExtension = index < 0 ? fileName : fileName.substring(0, index);
		return withoutExtension.replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim();
	}

	private String text(DocumentEntity document) {
		return document.getExtractedText() == null ? "" : document.getExtractedText().trim();
	}

	private String summary(String text) {
		if (text.isBlank()) {
			return "Resume a valider par le bibliothecaire.";
		}
		String normalized = text.replaceAll("\\s+", " ").trim();
		return normalized.length() <= 280 ? normalized : normalized.substring(0, 277).trim() + "...";
	}

	private String classification(List<String> keywords) {
		if (keywords.contains("Dublin Core")) {
			return "Sciences de l'information";
		}
		if (keywords.contains("multilingue")) {
			return "Technologies linguistiques";
		}
		return "Publication scientifique";
	}
}

package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.institution.InstitutionEntity;

import org.junit.jupiter.api.Test;

class LocalMetadataExtractionProviderTests {
	private final LocalMetadataExtractionProvider provider = new LocalMetadataExtractionProvider();
	private final InstitutionEntity institution = new InstitutionEntity("INST-A", "Institution A", "institution-a.example");

	@Test
	void returnsDublinCoreKeywordsForMetadataPublication() {
		MetadataExtractionData metadata = provider.extract(publication("Analyse automatique des metadonnees"));

		assertThat(metadata.title()).isEqualTo("Analyse automatique des metadonnees");
		assertThat(metadata.author()).isEqualTo("Auteur non renseigne");
		assertThat(metadata.keywords()).containsExactly("metadonnees", "Dublin Core", "catalogage");
	}

	@Test
	void returnsSearchKeywordsForMultilingualPublication() {
		MetadataExtractionData metadata = provider.extract(publication("Catalogue multilingue pour chercheurs"));

		assertThat(metadata.keywords()).containsExactly("multilingue", "indexation", "recherche");
	}

	@Test
	void returnsFallbackKeywordsForGeneralPublication() {
		MetadataExtractionData metadata = provider.extract(publication("Archivage institutionnel durable"));

		assertThat(metadata.keywords()).containsExactly("publication", "validation", "bibliotheque");
	}

	private DocumentEntity publication(String title) {
		return new DocumentEntity(
				title + ".txt",
				null,
				0L,
				"TXT",
				title,
				DocumentStatus.EN_ATTENTE,
				DocumentVisibility.PUBLIC,
				institution,
				null
		);
	}
}

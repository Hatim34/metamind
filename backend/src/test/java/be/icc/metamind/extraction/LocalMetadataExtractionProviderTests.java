package be.icc.metamind.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;

import org.junit.jupiter.api.Test;

class LocalMetadataExtractionProviderTests {
	private final LocalMetadataExtractionProvider provider = new LocalMetadataExtractionProvider();
	private final InstitutionEntity institution = new InstitutionEntity("INST-A", "Institution A", "institution-a.example");

	@Test
	void returnsDublinCoreKeywordsForMetadataPublication() {
		MetadataExtractionData metadata = provider.extract(publication("Analyse automatique des metadonnees"));

		assertThat(metadata.title()).isEqualTo("Analyse automatique des metadonnees");
		assertThat(metadata.author()).isEqualTo("Sarah Lemaire");
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

	private PublicationEntity publication(String title) {
		return new PublicationEntity(
				title,
				"Sarah Lemaire",
				2026,
				PublicationStatus.EN_ATTENTE,
				Visibility.PUBLIC,
				"",
				institution
		);
	}
}

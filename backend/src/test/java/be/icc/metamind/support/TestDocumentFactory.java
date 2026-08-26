package be.icc.metamind.support;

import java.time.LocalDate;
import java.util.List;

import be.icc.metamind.document.AuthorEntity;
import be.icc.metamind.document.AuthorRepository;
import be.icc.metamind.document.DocumentAuthorEntity;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.KeywordEntity;
import be.icc.metamind.document.KeywordRepository;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;
import be.icc.metamind.document.MetadataStatus;
import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.user.UserEntity;

public class TestDocumentFactory {
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final AuthorRepository authorRepository;
	private final KeywordRepository keywordRepository;
	private final DocumentAuthorRepository documentAuthorRepository;
	private final DocumentKeywordRepository documentKeywordRepository;

	public TestDocumentFactory(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			AuthorRepository authorRepository,
			KeywordRepository keywordRepository,
			DocumentAuthorRepository documentAuthorRepository,
			DocumentKeywordRepository documentKeywordRepository
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.authorRepository = authorRepository;
		this.keywordRepository = keywordRepository;
		this.documentAuthorRepository = documentAuthorRepository;
		this.documentKeywordRepository = documentKeywordRepository;
	}

	public DocumentEntity create(String title, String authorName, int year, DocumentStatus status, DocumentVisibility visibility, List<String> keywords, InstitutionEntity institution, UserEntity importedBy) {
		DocumentEntity document = documentRepository.save(new DocumentEntity(
				fileName(title),
				null,
				0L,
				"TXT",
				title,
				status,
				visibility,
				institution,
				importedBy
		));
		metadataRepository.save(new MetadataEntity(document, title, null, LocalDate.of(year, 1, 1), null, MetadataStatus.EN_ATTENTE));
		AuthorEntity author = authorRepository.findByFullNameIgnoreCase(authorName)
				.orElseGet(() -> authorRepository.save(new AuthorEntity(authorName, null)));
		documentAuthorRepository.save(new DocumentAuthorEntity(document, author, 1));
		for (String value : keywords) {
			KeywordEntity keyword = keywordRepository.findByLibelleIgnoreCase(value)
					.orElseGet(() -> keywordRepository.save(new KeywordEntity(value)));
			documentKeywordRepository.save(new DocumentKeywordEntity(document, keyword));
		}
		return document;
	}

	private String fileName(String title) {
		String normalized = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return normalized.isBlank() ? "document.txt" : normalized + ".txt";
	}
}

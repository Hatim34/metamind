package be.icc.metamind.opendata;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.DocumentAuthorRepository;
import be.icc.metamind.document.DocumentEntity;
import be.icc.metamind.document.DocumentKeywordRepository;
import be.icc.metamind.document.DocumentRepository;
import be.icc.metamind.document.DocumentStatus;
import be.icc.metamind.document.DocumentVisibility;
import be.icc.metamind.document.MetadataEntity;
import be.icc.metamind.document.MetadataRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenDataService {
	private final DocumentRepository documentRepository;
	private final MetadataRepository metadataRepository;
	private final DocumentAuthorRepository documentAuthorRepository;
	private final DocumentKeywordRepository documentKeywordRepository;
	private final String publicUrl;

	public OpenDataService(
			DocumentRepository documentRepository,
			MetadataRepository metadataRepository,
			DocumentAuthorRepository documentAuthorRepository,
			DocumentKeywordRepository documentKeywordRepository,
			@Value("${metamind.public-url:https://metamind-app.duckdns.org}") String publicUrl
	) {
		this.documentRepository = documentRepository;
		this.metadataRepository = metadataRepository;
		this.documentAuthorRepository = documentAuthorRepository;
		this.documentKeywordRepository = documentKeywordRepository;
		this.publicUrl = publicUrl.replaceAll("/+$", "");
	}

	@Transactional(readOnly = true)
	public String rssFeed() {
		List<DocumentEntity> documents = documentRepository.findAll()
				.stream()
				.filter(this::isPublicOpenData)
				.sorted(Comparator.comparing(DocumentEntity::getId).reversed())
				.toList();

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append("<rss version=\"2.0\"><channel>");
		xml.append("<title>Metamind - publications publiques</title>");
		xml.append("<link>").append(escape(publicUrl)).append("</link>");
		xml.append("<description>Flux public des publications enrichies dans Metamind</description>");
		for (DocumentEntity document : documents) {
			MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId()).orElse(null);
			xml.append("<item>");
			xml.append("<title>").append(escape(title(document, metadata))).append("</title>");
			xml.append("<author>").append(escape(authors(document))).append("</author>");
			xml.append("<link>").append(escape(publicationUrl(document))).append("</link>");
			xml.append("<guid>").append(escape(publicationUrl(document))).append("</guid>");
			xml.append("<pubDate>").append(metadata == null || metadata.getPublicationDate() == null ? "" : metadata.getPublicationDate().getYear()).append("</pubDate>");
			xml.append("<description>").append(escape(keywords(document))).append("</description>");
			xml.append("</item>");
		}
		xml.append("</channel></rss>");
		return xml.toString();
	}

	@Transactional(readOnly = true)
	public String dublinCore(long publicationId) {
		DocumentEntity document = documentRepository.findById(publicationId)
				.filter(this::isPublicOpenData)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication publique demandee est introuvable."));
		MetadataEntity metadata = metadataRepository.findByDocumentId(document.getId()).orElse(null);

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
		xml.append("<dc:title>").append(escape(title(document, metadata))).append("</dc:title>");
		xml.append("<dc:creator>").append(escape(authors(document))).append("</dc:creator>");
		xml.append("<dc:publisher>").append(escape(document.getInstitution().getName())).append("</dc:publisher>");
		xml.append("<dc:date>").append(metadata == null || metadata.getPublicationDate() == null ? "" : metadata.getPublicationDate().getYear()).append("</dc:date>");
		xml.append("<dc:type>").append(escape(metadata == null || metadata.getDocumentType() == null ? "Publication scientifique" : metadata.getDocumentType().getLibelle())).append("</dc:type>");
		for (String keyword : keywords(document).split(",")) {
			String value = keyword.trim();
			if (!value.isBlank()) {
				xml.append("<dc:subject>").append(escape(value)).append("</dc:subject>");
			}
		}
		xml.append("<dc:identifier>").append(escape(publicationUrl(document))).append("</dc:identifier>");
		xml.append("</metadata>");
		return xml.toString();
	}

	private boolean isPublicOpenData(DocumentEntity document) {
		return document.getStatus() == DocumentStatus.PUBLIE && document.getVisibility() == DocumentVisibility.PUBLIC;
	}

	private String publicationUrl(DocumentEntity document) {
		return publicUrl + "/api/v1/documents/" + document.getId();
	}

	private String title(DocumentEntity document, MetadataEntity metadata) {
		return metadata == null || metadata.getTitre() == null ? document.getFileName() : metadata.getTitre();
	}

	private String authors(DocumentEntity document) {
		String authors = documentAuthorRepository.findByDocument_IdOrderByAuthorOrderAsc(document.getId())
				.stream()
				.map(documentAuthor -> documentAuthor.getAuthor().getFullName())
				.collect(Collectors.joining(", "));
		return authors.isBlank() ? "Auteur non renseigne" : authors;
	}

	private String keywords(DocumentEntity document) {
		return documentKeywordRepository.findByDocument_Id(document.getId())
				.stream()
				.map(documentKeyword -> documentKeyword.getKeyword().getLibelle())
				.collect(Collectors.joining(","));
	}

	private String escape(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}

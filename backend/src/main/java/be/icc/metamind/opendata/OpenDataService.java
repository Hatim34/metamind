package be.icc.metamind.opendata;

import java.util.Comparator;
import java.util.List;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.publication.PublicationEntity;
import be.icc.metamind.publication.PublicationRepository;
import be.icc.metamind.publication.PublicationStatus;
import be.icc.metamind.publication.Visibility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenDataService {
	private final PublicationRepository publicationRepository;
	private final String publicUrl;

	public OpenDataService(
			PublicationRepository publicationRepository,
			@Value("${metamind.public-url:https://metamind-app.duckdns.org}") String publicUrl
	) {
		this.publicationRepository = publicationRepository;
		this.publicUrl = publicUrl.replaceAll("/+$", "");
	}

	@Transactional(readOnly = true)
	public String rssFeed() {
		List<PublicationEntity> publications = publicationRepository.findAll()
				.stream()
				.filter(this::isPublicOpenData)
				.sorted(Comparator.comparing(PublicationEntity::getYear).reversed())
				.toList();

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append("<rss version=\"2.0\"><channel>");
		xml.append("<title>Metamind - publications publiques</title>");
		xml.append("<link>").append(escape(publicUrl)).append("</link>");
		xml.append("<description>Flux public des publications enrichies dans Metamind</description>");
		for (PublicationEntity publication : publications) {
			xml.append("<item>");
			xml.append("<title>").append(escape(publication.getTitle())).append("</title>");
			xml.append("<author>").append(escape(publication.getAuthor())).append("</author>");
			xml.append("<link>").append(escape(publicationUrl(publication))).append("</link>");
			xml.append("<guid>").append(escape(publicationUrl(publication))).append("</guid>");
			xml.append("<pubDate>").append(publication.getYear()).append("</pubDate>");
			xml.append("<description>").append(escape(publication.getKeywordsText())).append("</description>");
			xml.append("</item>");
		}
		xml.append("</channel></rss>");
		return xml.toString();
	}

	@Transactional(readOnly = true)
	public String dublinCore(long publicationId) {
		PublicationEntity publication = publicationRepository.findById(publicationId)
				.filter(this::isPublicOpenData)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La publication publique demandee est introuvable."));

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
		xml.append("<dc:title>").append(escape(publication.getTitle())).append("</dc:title>");
		xml.append("<dc:creator>").append(escape(publication.getAuthor())).append("</dc:creator>");
		xml.append("<dc:publisher>").append(escape(publication.getInstitution().getName())).append("</dc:publisher>");
		xml.append("<dc:date>").append(publication.getYear()).append("</dc:date>");
		xml.append("<dc:type>Publication scientifique</dc:type>");
		for (String keyword : publication.getKeywordsText().split(",")) {
			String value = keyword.trim();
			if (!value.isBlank()) {
				xml.append("<dc:subject>").append(escape(value)).append("</dc:subject>");
			}
		}
		xml.append("<dc:identifier>").append(escape(publicationUrl(publication))).append("</dc:identifier>");
		xml.append("</metadata>");
		return xml.toString();
	}

	private boolean isPublicOpenData(PublicationEntity publication) {
		return publication.getStatus() == PublicationStatus.PUBLIE && publication.getVisibility() == Visibility.PUBLIC;
	}

	private String publicationUrl(PublicationEntity publication) {
		return publicUrl + "/api/v1/publications/" + publication.getId();
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

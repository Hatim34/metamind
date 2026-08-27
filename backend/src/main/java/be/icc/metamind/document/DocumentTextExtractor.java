package be.icc.metamind.document;

import java.io.IOException;
import java.io.InputStream;

import be.icc.metamind.api.ApiException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

@Service
public class DocumentTextExtractor {
	private final Parser parser = new AutoDetectParser();

	public String extract(MultipartFile file) {
		Metadata metadata = new Metadata();
		metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
		BodyContentHandler handler = new BodyContentHandler(-1);

		try (InputStream input = file.getInputStream()) {
			parser.parse(input, handler, metadata, new ParseContext());
			return normalize(handler.toString());
		}
		catch (IOException | TikaException | SAXException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Le contenu du fichier n'a pas pu etre lu.");
		}
	}

	private String normalize(String text) {
		return text == null ? "" : text.replaceAll("[\\t\\x0B\\f\\r ]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
	}
}

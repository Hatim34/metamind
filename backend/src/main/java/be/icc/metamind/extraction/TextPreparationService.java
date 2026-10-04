package be.icc.metamind.extraction;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TextPreparationService {
	private static final Pattern DOI_PATTERN = Pattern.compile(
			"(?i)\\b10\\.\\d{4,9}/[-._;()/:a-z0-9]+"
	);
	private static final Pattern ORCID_PATTERN = Pattern.compile(
			"\\b\\d{4}-\\d{4}-\\d{4}-[\\dX]{4}\\b", Pattern.CASE_INSENSITIVE
	);
	private static final int DEFAULT_MAX_INPUT_CHARS = 15_000;
	private static final int MAX_SEGMENT_CHARS = 1_500;
	/** Zone ou figure le DOI propre a une publication (page de titre, en-tete d'editeur). */
	private static final int FRONT_MATTER_CHARS = 3_000;
	private static final Pattern REFERENCES_HEADING = Pattern.compile(
			"(?im)^\\s*(references?|bibliographie|bibliography|referenties|literatur|works cited)\\s*:?\\s*$"
	);

	private record TypeCue(String type, List<String> markers) {
	}

	/** Indices ordonnes du plus specifique au plus general (le premier qui correspond gagne). */
	private static final List<TypeCue> DOCUMENT_TYPE_CUES = List.of(
			new TypeCue("these", List.of(
					"these de doctorat", "these presentee", "doctoral thesis", "phd thesis",
					"doctor of philosophy", "proefschrift", "doctoraatsproefschrift"
			)),
			new TypeCue("memoire", List.of(
					"memoire de master", "memoire presente", "master thesis", "master's thesis",
					"masterproef", "bachelorproef", "travail de fin d etudes", "travail de fin d etude"
			)),
			new TypeCue("preprint", List.of(
					"preprint", "arxiv:", "biorxiv", "not peer reviewed", "submitted for publication"
			)),
			new TypeCue("chapitre", List.of(
					"chapitre de livre", "book chapter", "boekhoofdstuk", "in: handbook", "chapter in"
			)),
			new TypeCue("communication", List.of(
					"actes du colloque", "actes de la conference", "conference proceedings",
					"proceedings of the", "communication presentee", "congresbijdrage"
			)),
			new TypeCue("rapport", List.of(
					"rapport technique", "rapport de recherche", "rapport annuel", "technical report",
					"working paper", "onderzoeksrapport", "research report"
			)),
			new TypeCue("article", List.of(
					"journal of", "peer-reviewed", "peer reviewed", "publie dans la revue",
					"tijdschrift", "received:", "accepted:"
			))
	);
	private final int defaultMaxInputChars;

	public TextPreparationService(@Value("${metamind.llm.max-input-chars:15000}") int defaultMaxInputChars) {
		this.defaultMaxInputChars = Math.max(1_000, defaultMaxInputChars);
	}

	public PreparedDocument prepare(String sourceText) {
		return prepare(sourceText, defaultMaxInputChars);
	}

	public PreparedDocument prepare(String sourceText, int maxInputChars) {
		String normalized = normalize(sourceText);
		String excerpt = excerpt(normalized, Math.max(1_000, maxInputChars));
		return new PreparedDocument(
				normalized,
				excerpt,
				segments(excerpt),
				detectLanguage(normalized),
				detectDocumentType(excerpt),
				detectDocumentDoi(normalized),
				detect(DOI_PATTERN, normalized),
				detect(ORCID_PATTERN, normalized)
		);
	}

	String normalize(String sourceText) {
		if (sourceText == null || sourceText.isBlank()) {
			return "";
		}
		String text = sourceText.replace("\r\n", "\n").replace('\r', '\n')
				.replaceAll("(?<=\\p{L})-\\s*\\n\\s*(?=\\p{L})", "");
		List<String> lines = text.lines().map(line -> line.replaceAll("[\\t\\f ]+", " ").trim()).toList();
		Map<String, Long> occurrences = lines.stream()
				.filter(line -> !line.isBlank() && line.length() <= 120)
				.collect(Collectors.groupingBy(
						line -> line.toLowerCase(Locale.ROOT), Collectors.counting()
				));
		return lines.stream()
				.filter(line -> occurrences.getOrDefault(line.toLowerCase(Locale.ROOT), 0L) < 3)
				.collect(Collectors.joining("\n"))
				.replaceAll("\\n{3,}", "\n\n")
				.trim();
	}

	private String excerpt(String text, int maxInputChars) {
		if (text.length() <= maxInputChars) {
			return text;
		}
		String separator = "\n\n[... extrait tronque ...]\n\n";
		int available = maxInputChars - separator.length();
		int firstLength = Math.min(12_000, Math.max(1, (available * 4) / 5));
		int lastLength = Math.max(1, available - firstLength);
		return text.substring(0, firstLength).trim() + separator
				+ text.substring(text.length() - lastLength).trim();
	}

	private List<PreparedDocument.Segment> segments(String text) {
		if (text.isBlank()) {
			return List.of();
		}
		List<String> chunks = new ArrayList<>();
		for (String paragraph : text.split("\\n\\s*\\n")) {
			String clean = paragraph.replaceAll("\\s+", " ").trim();
			while (clean.length() > MAX_SEGMENT_CHARS) {
				int splitAt = clean.lastIndexOf(' ', MAX_SEGMENT_CHARS);
				int cut = splitAt > 0 ? splitAt : MAX_SEGMENT_CHARS;
				chunks.add(clean.substring(0, cut).trim());
				clean = clean.substring(cut).trim();
			}
			if (!clean.isBlank()) {
				chunks.add(clean);
			}
		}
		List<PreparedDocument.Segment> segments = new ArrayList<>();
		for (int index = 0; index < chunks.size(); index++) {
			segments.add(new PreparedDocument.Segment("S" + (index + 1), chunks.get(index)));
		}
		return List.copyOf(segments);
	}

	private Set<String> detect(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		Set<String> values = new LinkedHashSet<>();
		while (matcher.find()) {
			values.add(trimTrailingPunctuation(matcher.group()));
		}
		// LinkedHashSet immuable : Set.copyOf perdrait l'ordre d'apparition.
		return Collections.unmodifiableSet(values);
	}

	/**
	 * DOI du document lui-meme, cherche uniquement dans l'en-tete.
	 * Un article cite souvent des dizaines de DOI en bibliographie : les retenir
	 * attribuerait au document le DOI d'un travail reference.
	 */
	String detectDocumentDoi(String normalizedText) {
		if (normalizedText == null || normalizedText.isBlank()) {
			return null;
		}
		String frontMatter = normalizedText.length() <= FRONT_MATTER_CHARS
				? normalizedText
				: normalizedText.substring(0, FRONT_MATTER_CHARS);
		String beforeReferences = stripReferenceSection(frontMatter);
		Matcher matcher = DOI_PATTERN.matcher(beforeReferences);
		return matcher.find() ? trimTrailingPunctuation(matcher.group()) : null;
	}

	/** Coupe a la premiere rubrique de references, si elle apparait des l'en-tete. */
	private String stripReferenceSection(String text) {
		Matcher matcher = REFERENCES_HEADING.matcher(text);
		return matcher.find() ? text.substring(0, matcher.start()) : text;
	}

	private String trimTrailingPunctuation(String value) {
		return value.replaceAll("[.,;:]+$", "");
	}

	/**
	 * Deduit le type de document a partir d'indices presents dans l'extrait.
	 * Retourne null quand aucun indice fiable n'est trouve : aucune valeur n'est inventee.
	 */
	String detectDocumentType(String excerpt) {
		if (excerpt == null || excerpt.isBlank()) {
			return null;
		}
		String haystack = foldAccents(excerpt.toLowerCase(Locale.ROOT)).replaceAll("\\s+", " ");
		return DOCUMENT_TYPE_CUES.stream()
				.filter(cue -> cue.markers().stream().anyMatch(haystack::contains))
				.map(TypeCue::type)
				.findFirst()
				.orElse(null);
	}

	private String foldAccents(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	private String detectLanguage(String text) {
		try {
			LanguageDetector detector = LanguageDetector.getDefaultLanguageDetector();
			LanguageResult result = detector.detect(text);
			if (result.isReasonablyCertain() && Set.of("fr", "nl", "en").contains(result.getLanguage())) {
				return result.getLanguage();
			}
		} catch (RuntimeException ignored) {
			// Tika can be present without a language-model implementation.
		}
		return fallbackLanguage(text);
	}

	private String fallbackLanguage(String text) {
		Map<String, Set<String>> stopWords = Map.of(
				"fr", Set.of("le", "la", "les", "des", "pour", "avec", "dans", "une"),
				"nl", Set.of("de", "het", "een", "van", "voor", "met", "in", "en"),
				"en", Set.of("the", "and", "for", "with", "from", "this", "that", "of")
		);
		Set<String> words = Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+"))
				.filter(word -> !word.isBlank())
				.collect(Collectors.toSet());
		return stopWords.entrySet().stream()
				.max(Comparator.comparingInt(entry -> (int) entry.getValue().stream().filter(words::contains).count()))
				.map(Map.Entry::getKey)
				.orElse("en");
	}
}

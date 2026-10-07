package be.icc.metamind.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.document.ConfigurationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parametres modifiables par l'administrateur (cahier des charges A2), reellement appliques.
 *
 * Chaque valeur est relue a l'usage : une modification prend effet sans redemarrage.
 * Sans valeur enregistree, le reglage de deploiement (variable d'environnement) s'applique.
 * Les cles API et les reglages lies au deploiement (fournisseur LLM, Stripe, DSpace)
 * restent dans les variables d'environnement et ne sont jamais modifiables ici.
 */
@Service
public class PlatformSettings {
	public static final String LLM_MODEL = "modele_llm";
	public static final String MAX_UPLOAD_MB = "taille_max_upload_mo";
	public static final String MAX_LOGIN_FAILURES = "tentatives_connexion_max";
	public static final String SESSION_SECONDS = "jwt_duree_secondes";

	/** Plafond technique : la configuration Spring refuse au-dela de 50 Mo par requete. */
	public static final int UPLOAD_CEILING_MB = 50;
	private static final Pattern GEMINI_MODEL = Pattern.compile("^gemini-[a-z0-9][a-z0-9.\\-]{2,60}$");

	private final ConfigurationRepository configurationRepository;
	private final String defaultModel;
	private final long defaultSessionSeconds;

	public PlatformSettings(
			ConfigurationRepository configurationRepository,
			@Value("${metamind.gemini.model:gemini-3.5-flash-lite}") String defaultModel,
			@Value("${metamind.jwt.duration-seconds:3600}") long defaultSessionSeconds
	) {
		this.configurationRepository = configurationRepository;
		this.defaultModel = defaultModel;
		this.defaultSessionSeconds = defaultSessionSeconds;
	}

	@Transactional(readOnly = true)
	public String llmModel() {
		return stored(LLM_MODEL).filter(value -> GEMINI_MODEL.matcher(value).matches()).orElse(defaultModel);
	}

	@Transactional(readOnly = true)
	public long maxUploadBytes() {
		return (long) intValue(MAX_UPLOAD_MB, UPLOAD_CEILING_MB, 1, UPLOAD_CEILING_MB) * 1024L * 1024L;
	}

	@Transactional(readOnly = true)
	public int maxLoginFailures() {
		return intValue(MAX_LOGIN_FAILURES, 5, 3, 20);
	}

	@Transactional(readOnly = true)
	public long sessionSeconds() {
		return intValue(SESSION_SECONDS, (int) defaultSessionSeconds, 900, 86_400);
	}

	/** Valeurs en vigueur, telles que la page d'administration les affiche. */
	@Transactional(readOnly = true)
	public Map<String, String> current() {
		Map<String, String> values = new LinkedHashMap<>();
		values.put(LLM_MODEL, llmModel());
		values.put(MAX_UPLOAD_MB, String.valueOf(maxUploadBytes() / (1024L * 1024L)));
		values.put(MAX_LOGIN_FAILURES, String.valueOf(maxLoginFailures()));
		values.put(SESSION_SECONDS, String.valueOf(sessionSeconds()));
		return values;
	}

	/** Refuse une valeur qui ne serait pas appliquee, plutot que de l'enregistrer sans effet. */
	public void validate(String key, String value) {
		switch (key) {
			case LLM_MODEL -> {
				if (!GEMINI_MODEL.matcher(value).matches()) {
					throw invalid("Le modele doit etre un modele Gemini, par exemple gemini-3.5-flash-lite.");
				}
			}
			case MAX_UPLOAD_MB -> requireRange(value, 1, UPLOAD_CEILING_MB, "La taille maximale doit etre comprise entre 1 et 50 Mo.");
			case MAX_LOGIN_FAILURES -> requireRange(value, 3, 20, "Le nombre d'echecs avant blocage doit etre compris entre 3 et 20.");
			case SESSION_SECONDS -> requireRange(value, 900, 86_400, "La duree de session doit etre comprise entre 900 et 86400 secondes.");
			default -> throw invalid("Ce parametre n'est pas modifiable.");
		}
	}

	public static boolean isEditable(String key) {
		return LLM_MODEL.equals(key) || MAX_UPLOAD_MB.equals(key) || MAX_LOGIN_FAILURES.equals(key) || SESSION_SECONDS.equals(key);
	}

	private Optional<String> stored(String key) {
		return configurationRepository.findById(key)
				.map(configuration -> configuration.getValeur() == null ? "" : configuration.getValeur().trim())
				.filter(value -> !value.isEmpty());
	}

	private int intValue(String key, int fallback, int min, int max) {
		return stored(key)
				.flatMap(PlatformSettings::parse)
				.filter(value -> value >= min && value <= max)
				.orElse(fallback);
	}

	private static Optional<Integer> parse(String value) {
		try {
			return Optional.of(Integer.parseInt(value));
		}
		catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	private static void requireRange(String value, int min, int max, String message) {
		int parsed = parse(value).orElseThrow(() -> invalid(message));
		if (parsed < min || parsed > max) {
			throw invalid(message);
		}
	}

	private static ApiException invalid(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}
}

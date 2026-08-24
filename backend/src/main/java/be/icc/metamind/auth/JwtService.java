package be.icc.metamind.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import be.icc.metamind.api.ApiException;
import be.icc.metamind.user.UserEntity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
	private static final Pattern NUMBER_FIELD = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");

	private final String secret;
	private final long durationSeconds;

	public JwtService(
			@Value("${metamind.jwt.secret:change-this-development-secret-for-production}") String secret,
			@Value("${metamind.jwt.duration-seconds:3600}") long durationSeconds
	) {
		this.secret = secret;
		this.durationSeconds = durationSeconds;
	}

	public String createToken(UserEntity user) {
		long expiration = Instant.now().plusSeconds(durationSeconds).getEpochSecond();
		String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
		String payload = encode("{\"sub\":" + user.getId()
				+ ",\"role\":\"" + user.getRole().name()
				+ "\",\"institutionId\":" + user.getInstitution().getId()
				+ ",\"exp\":" + expiration + "}");
		String unsignedToken = header + "." + payload;
		return unsignedToken + "." + sign(unsignedToken);
	}

	public long readUserId(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le jeton d'authentification est manquant.");
		}

		String token = authorizationHeader.substring("Bearer ".length()).trim();
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le jeton d'authentification est invalide.");
		}

		String unsignedToken = parts[0] + "." + parts[1];
		if (!sign(unsignedToken).equals(parts[2])) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "La signature du jeton est invalide.");
		}

		String payload = decode(parts[1]);
		long expiration = readLongField(payload, "exp");
		if (Instant.now().getEpochSecond() > expiration) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le jeton d'authentification a expire.");
		}

		return readLongField(payload, "sub");
	}

	private String encode(String value) {
		return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String decode(String encodedValue) {
		try {
			return new String(DECODER.decode(encodedValue), StandardCharsets.UTF_8);
		}
		catch (Exception exception) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le contenu du jeton est invalide.");
		}
	}

	private long readLongField(String payload, String fieldName) {
		Matcher matcher = Pattern.compile(NUMBER_FIELD.pattern().formatted(fieldName)).matcher(payload);
		if (!matcher.find()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Le contenu du jeton est incomplet.");
		}
		return Long.parseLong(matcher.group(1));
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception exception) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "La signature du jeton a echoue.");
		}
	}
}

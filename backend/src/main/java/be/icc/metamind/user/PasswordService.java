package be.icc.metamind.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class PasswordService {
	public String hash(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] value = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(value);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Algorithme de hachage indisponible.", exception);
		}
	}

	public boolean matches(String password, String hash) {
		return hash(password).equals(hash);
	}
}

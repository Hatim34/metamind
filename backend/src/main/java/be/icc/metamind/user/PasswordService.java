package be.icc.metamind.user;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class PasswordService {
	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	public String hash(String password) {
		return encoder.encode(password);
	}

	public boolean matches(String password, String hash) {
		return encoder.matches(password, hash);
	}
}

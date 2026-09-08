package be.icc.metamind.auth;

import be.icc.metamind.user.AccountService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import be.icc.metamind.user.PasswordResetService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AccountService service;
	private final PasswordResetService passwordResetService;

	public AuthController(AccountService service, PasswordResetService passwordResetService) {
		this.service = service;
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return service.login(request);
	}

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return service.register(request);
	}

	@PostMapping("/password-reset/request")
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		passwordResetService.requestReset(request);
	}

	@PostMapping("/password-reset/confirm")
	public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
		passwordResetService.confirmReset(request);
	}
}

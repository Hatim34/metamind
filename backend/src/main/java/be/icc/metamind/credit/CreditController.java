package be.icc.metamind.credit;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/credits")
public class CreditController {
	private final CreditService service;

	public CreditController(CreditService service) {
		this.service = service;
	}

	@GetMapping
	public CreditBalanceResponse balance(@PathVariable long userId) {
		return service.getBalance(userId);
	}

	@PostMapping("/purchase")
	public CreditBalanceResponse purchase(@PathVariable long userId, @Valid @RequestBody CreditPurchaseRequest request) {
		return service.purchase(userId, request);
	}
}

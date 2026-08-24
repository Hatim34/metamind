package be.icc.metamind.credit;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import be.icc.metamind.user.AccountService;

@RestController
@RequestMapping("/api/v1/users/{userId}/credits")
public class CreditController {
	private final CreditService service;
	private final AccountService accountService;

	public CreditController(CreditService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@GetMapping
	public CreditBalanceResponse balance(@PathVariable long userId, @RequestHeader("Authorization") String authorization) {
		accountService.authenticateSelfOrAdmin(userId, authorization);
		return service.getBalance(userId);
	}

	@PostMapping("/purchase")
	public CreditBalanceResponse purchase(@PathVariable long userId, @RequestHeader("Authorization") String authorization, @Valid @RequestBody CreditPurchaseRequest request) {
		accountService.authenticateSelfOrAdmin(userId, authorization);
		return service.purchase(userId, request);
	}
}

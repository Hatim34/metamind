package be.icc.metamind.credit;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import be.icc.metamind.user.AccountService;
import be.icc.metamind.user.UserEntity;

@RestController
@RequestMapping("/api/v1")
public class CreditController {
	private final CreditService service;
	private final AccountService accountService;

	public CreditController(CreditService service, AccountService accountService) {
		this.service = service;
		this.accountService = accountService;
	}

	@GetMapping("/credits")
	public CreditAccountResponse currentAccount(@RequestHeader("Authorization") String authorization) {
		UserEntity user = accountService.authenticate(authorization);
		return service.getCurrentAccount(user);
	}

	@GetMapping("/credits/packs")
	public List<CreditPackOptionResponse> packs() {
		return service.listPacks();
	}

	@PostMapping("/credits")
	public CreditCheckoutResponse checkout(@RequestHeader("Authorization") String authorization, @Valid @RequestBody CreditCheckoutRequest request) {
		UserEntity user = accountService.authenticate(authorization);
		return service.startCheckout(user, request);
	}

	@PostMapping("/webhooks/stripe")
	public CreditBalanceResponse stripeWebhook(@RequestBody StripeWebhookRequest request) {
		return service.confirmStripePayment(request);
	}

	@PostMapping("/admin/institutions/{institutionId}/credits/adjustments")
	public CreditBalanceResponse adjustCredits(@PathVariable long institutionId, @RequestHeader("Authorization") String authorization, @Valid @RequestBody AdminCreditAdjustmentRequest request) {
		UserEntity admin = accountService.authenticateAdmin(authorization);
		return service.adjustInstitutionCredits(admin, institutionId, request);
	}

	@GetMapping("/users/{userId}/credits")
	public CreditBalanceResponse balance(@PathVariable long userId, @RequestHeader("Authorization") String authorization) {
		accountService.authenticateSelfOrAdmin(userId, authorization);
		return service.getBalance(userId);
	}

	@PostMapping("/users/{userId}/credits/purchase")
	public CreditBalanceResponse purchase(@PathVariable long userId, @RequestHeader("Authorization") String authorization, @Valid @RequestBody CreditPurchaseRequest request) {
		accountService.authenticateSelfOrAdmin(userId, authorization);
		return service.purchase(userId, request);
	}

	@GetMapping("/users/{userId}/credits/movements")
	public List<CreditMovementResponse> movements(@PathVariable long userId, @RequestHeader("Authorization") String authorization) {
		accountService.authenticateSelfOrAdmin(userId, authorization);
		return service.listMovements(userId);
	}
}

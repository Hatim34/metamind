package be.icc.metamind.credit;

public record StripeWebhookRequest(
		String reference,
		String type
) {
}

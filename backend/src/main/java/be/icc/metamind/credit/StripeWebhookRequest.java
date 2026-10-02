package be.icc.metamind.credit;

public record StripeWebhookRequest(
		String reference,
		String type,
		String eventId,
		Long amountTotal,
		String currency
) {
	public StripeWebhookRequest(String reference, String type) {
		this(reference, type, null, null, null);
	}

	public StripeWebhookRequest(String reference, String type, String eventId) {
		this(reference, type, eventId, null, null);
	}
}

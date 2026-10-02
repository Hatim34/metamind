package be.icc.metamind.credit;

public record StripeWebhookRequest(
		String reference,
		String type,
		String eventId
) {
	public StripeWebhookRequest(String reference, String type) {
		this(reference, type, null);
	}
}

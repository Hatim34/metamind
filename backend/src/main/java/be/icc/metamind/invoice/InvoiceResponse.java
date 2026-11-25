package be.icc.metamind.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InvoiceResponse(
		long id,
		String numero,
		InvoiceStatus statut,
		@JsonProperty("date_emission") LocalDate issueDate,
		String description,
		@JsonProperty("montant_ht") BigDecimal netAmount,
		@JsonProperty("taux_tva") BigDecimal vatRate,
		@JsonProperty("montant_tva") BigDecimal vatAmount,
		@JsonProperty("montant_ttc") BigDecimal grossAmount,
		String devise,
		String institution,
		@JsonProperty("reference_paiement") String paymentReference
) {
	public static InvoiceResponse from(InvoiceEntity invoice) {
		return new InvoiceResponse(invoice.getId(), invoice.getNumber(), invoice.getStatus(), invoice.getIssueDate(),
				invoice.getDescription(), invoice.getNetAmount(), invoice.getVatRate(), invoice.getVatAmount(), invoice.getGrossAmount(),
				invoice.getCurrency(), invoice.getInstitution().getName(), invoice.getPack().getPaymentReference());
	}
}

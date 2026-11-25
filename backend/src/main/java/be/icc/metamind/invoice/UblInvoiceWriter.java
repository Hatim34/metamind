package be.icc.metamind.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Ecrit une facture au format Peppol BIS Billing 3.0 (syntaxe UBL 2.1, norme europeenne EN 16931),
 * le format obligatoire pour les factures entre entreprises belges depuis 2026.
 *
 * L'achat est deja paye par carte via Stripe : le montant prepaye vaut le total TTC et le montant
 * restant du est nul. L'ordre des elements suit le schema UBL, qui l'impose.
 */
@Component
public class UblInvoiceWriter {
	static final String CUSTOMIZATION_ID = "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0";
	static final String PROFILE_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";
	/** Schema d'identification Peppol du numero d'entreprise belge (BCE). */
	static final String BELGIAN_ENTERPRISE_SCHEME = "0208";

	private final SellerSettings seller;

	public UblInvoiceWriter(SellerSettings seller) {
		this.seller = seller;
	}

	public String write(InvoiceEntity invoice) {
		String currency = invoice.getCurrency();
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"")
				.append(" xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"")
				.append(" xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");
		element(xml, 1, "cbc:CustomizationID", CUSTOMIZATION_ID);
		element(xml, 1, "cbc:ProfileID", PROFILE_ID);
		element(xml, 1, "cbc:ID", invoice.getNumber());
		element(xml, 1, "cbc:IssueDate", invoice.getIssueDate().toString());
		element(xml, 1, "cbc:InvoiceTypeCode", "380");
		element(xml, 1, "cbc:Note", seller.demonstration()
				? "Facture de démonstration, sans valeur légale. Payée par carte via Stripe."
				: "Payée par carte via Stripe.");
		element(xml, 1, "cbc:DocumentCurrencyCode", currency);
		element(xml, 1, "cbc:BuyerReference", invoice.getPack().getPaymentReference());

		open(xml, 1, "cac:AccountingSupplierParty");
		party(xml, seller.name(), seller.enterpriseNumber(), seller.street(), seller.postalCode(), seller.city(), seller.country(), seller.email());
		close(xml, 1, "cac:AccountingSupplierParty");

		open(xml, 1, "cac:AccountingCustomerParty");
		party(xml, invoice.getBuyerName(), invoice.getBuyerEnterpriseNumber(), invoice.getBuyerStreet(), invoice.getBuyerPostalCode(),
				invoice.getBuyerCity(), invoice.getBuyerCountry(), null);
		close(xml, 1, "cac:AccountingCustomerParty");

		open(xml, 1, "cac:TaxTotal");
		amount(xml, 2, "cbc:TaxAmount", invoice.getVatAmount(), currency);
		open(xml, 2, "cac:TaxSubtotal");
		amount(xml, 3, "cbc:TaxableAmount", invoice.getNetAmount(), currency);
		amount(xml, 3, "cbc:TaxAmount", invoice.getVatAmount(), currency);
		taxCategory(xml, 3, "cac:TaxCategory", invoice.getVatRate());
		close(xml, 2, "cac:TaxSubtotal");
		close(xml, 1, "cac:TaxTotal");

		open(xml, 1, "cac:LegalMonetaryTotal");
		amount(xml, 2, "cbc:LineExtensionAmount", invoice.getNetAmount(), currency);
		amount(xml, 2, "cbc:TaxExclusiveAmount", invoice.getNetAmount(), currency);
		amount(xml, 2, "cbc:TaxInclusiveAmount", invoice.getGrossAmount(), currency);
		amount(xml, 2, "cbc:PrepaidAmount", invoice.getGrossAmount(), currency);
		amount(xml, 2, "cbc:PayableAmount", BigDecimal.ZERO, currency);
		close(xml, 1, "cac:LegalMonetaryTotal");

		open(xml, 1, "cac:InvoiceLine");
		element(xml, 2, "cbc:ID", "1");
		indent(xml, 2).append("<cbc:InvoicedQuantity unitCode=\"C62\">").append(invoice.getQuantity()).append("</cbc:InvoicedQuantity>\n");
		amount(xml, 2, "cbc:LineExtensionAmount", invoice.getNetAmount(), currency);
		open(xml, 2, "cac:Item");
		element(xml, 3, "cbc:Name", invoice.getDescription());
		taxCategory(xml, 3, "cac:ClassifiedTaxCategory", invoice.getVatRate());
		close(xml, 2, "cac:Item");
		open(xml, 2, "cac:Price");
		amount(xml, 3, "cbc:PriceAmount", invoice.getNetAmount(), currency);
		close(xml, 2, "cac:Price");
		close(xml, 1, "cac:InvoiceLine");

		xml.append("</Invoice>\n");
		return xml.toString();
	}

	private void party(StringBuilder xml, String name, String enterpriseNumber, String street, String postalCode, String city,
			String country, String email) {
		open(xml, 2, "cac:Party");
		indent(xml, 3).append("<cbc:EndpointID schemeID=\"").append(BELGIAN_ENTERPRISE_SCHEME).append("\">")
				.append(escape(enterpriseNumber)).append("</cbc:EndpointID>\n");
		open(xml, 3, "cac:PartyName");
		element(xml, 4, "cbc:Name", name);
		close(xml, 3, "cac:PartyName");
		open(xml, 3, "cac:PostalAddress");
		element(xml, 4, "cbc:StreetName", street);
		element(xml, 4, "cbc:CityName", city);
		element(xml, 4, "cbc:PostalZone", postalCode);
		open(xml, 4, "cac:Country");
		element(xml, 5, "cbc:IdentificationCode", country);
		close(xml, 4, "cac:Country");
		close(xml, 3, "cac:PostalAddress");
		open(xml, 3, "cac:PartyTaxScheme");
		element(xml, 4, "cbc:CompanyID", country + enterpriseNumber);
		open(xml, 4, "cac:TaxScheme");
		element(xml, 5, "cbc:ID", "VAT");
		close(xml, 4, "cac:TaxScheme");
		close(xml, 3, "cac:PartyTaxScheme");
		open(xml, 3, "cac:PartyLegalEntity");
		element(xml, 4, "cbc:RegistrationName", name);
		indent(xml, 4).append("<cbc:CompanyID schemeID=\"").append(BELGIAN_ENTERPRISE_SCHEME).append("\">")
				.append(escape(enterpriseNumber)).append("</cbc:CompanyID>\n");
		close(xml, 3, "cac:PartyLegalEntity");
		if (email != null && !email.isBlank()) {
			open(xml, 3, "cac:Contact");
			element(xml, 4, "cbc:ElectronicMail", email);
			close(xml, 3, "cac:Contact");
		}
		close(xml, 2, "cac:Party");
	}

	private void taxCategory(StringBuilder xml, int depth, String tag, BigDecimal rate) {
		open(xml, depth, tag);
		element(xml, depth + 1, "cbc:ID", "S");
		element(xml, depth + 1, "cbc:Percent", rate.stripTrailingZeros().toPlainString());
		open(xml, depth + 1, "cac:TaxScheme");
		element(xml, depth + 2, "cbc:ID", "VAT");
		close(xml, depth + 1, "cac:TaxScheme");
		close(xml, depth, tag);
	}

	private void amount(StringBuilder xml, int depth, String tag, BigDecimal value, String currency) {
		indent(xml, depth).append('<').append(tag).append(" currencyID=\"").append(currency).append("\">")
				.append(value.setScale(2, RoundingMode.HALF_UP).toPlainString()).append("</").append(tag).append(">\n");
	}

	private void element(StringBuilder xml, int depth, String tag, String value) {
		indent(xml, depth).append('<').append(tag).append('>').append(escape(value)).append("</").append(tag).append(">\n");
	}

	private void open(StringBuilder xml, int depth, String tag) {
		indent(xml, depth).append('<').append(tag).append(">\n");
	}

	private void close(StringBuilder xml, int depth, String tag) {
		indent(xml, depth).append("</").append(tag).append(">\n");
	}

	private StringBuilder indent(StringBuilder xml, int depth) {
		return xml.append("\t".repeat(depth));
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}

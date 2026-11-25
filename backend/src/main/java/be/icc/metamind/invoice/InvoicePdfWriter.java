package be.icc.metamind.invoice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/** Version lisible de la facture, avec les memes donnees que le XML Peppol et les mentions legales. */
@Component
public class InvoicePdfWriter {
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final float LEFT = 56;

	private final SellerSettings seller;

	public InvoicePdfWriter(SellerSettings seller) {
		this.seller = seller;
	}

	public byte[] write(InvoiceEntity invoice) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			float top = page.getMediaBox().getHeight() - 60;
			try (PDPageContentStream text = new PDPageContentStream(document, page)) {
				line(text, bold, 20, LEFT, top, "Facture " + invoice.getNumber());
				line(text, regular, 10, LEFT, top - 18, "Date d'emission : " + invoice.getIssueDate().format(DATE)
						+ "    Reference du paiement : " + invoice.getPack().getPaymentReference());
				if (seller.demonstration()) {
					line(text, bold, 10, LEFT, top - 34, "Facture de demonstration, sans valeur legale (projet academique).");
				}

				float y = top - 70;
				line(text, bold, 11, LEFT, y, "Vendeur");
				line(text, bold, 11, 320, y, "Acheteur");
				String[] sellerLines = {seller.name(), seller.street(), seller.postalCode() + " " + seller.city() + " (" + seller.country() + ")",
						"BCE " + EnterpriseNumber.format(seller.enterpriseNumber()), "TVA " + seller.country() + seller.enterpriseNumber()};
				String[] buyerLines = {invoice.getBuyerName(), invoice.getBuyerStreet(),
						invoice.getBuyerPostalCode() + " " + invoice.getBuyerCity() + " (" + invoice.getBuyerCountry() + ")",
						"BCE " + EnterpriseNumber.format(invoice.getBuyerEnterpriseNumber()),
						"TVA " + invoice.getBuyerCountry() + invoice.getBuyerEnterpriseNumber()};
				for (int index = 0; index < sellerLines.length; index++) {
					line(text, regular, 10, LEFT, y - 16 - 14 * index, clip(sellerLines[index], 48));
					line(text, regular, 10, 320, y - 16 - 14 * index, clip(buyerLines[index], 48));
				}

				y -= 120;
				line(text, bold, 10, LEFT, y, "Description");
				line(text, bold, 10, 330, y, "Qte");
				line(text, bold, 10, 380, y, "Prix HT");
				line(text, bold, 10, 460, y, "Total HT");
				line(text, regular, 10, LEFT, y - 18, clip(invoice.getDescription(), 52));
				line(text, regular, 10, 330, y - 18, String.valueOf(invoice.getQuantity()));
				line(text, regular, 10, 380, y - 18, euros(invoice.getNetAmount()));
				line(text, regular, 10, 460, y - 18, euros(invoice.getNetAmount()));

				y -= 70;
				line(text, regular, 10, 330, y, "Total HT");
				line(text, regular, 10, 460, y, euros(invoice.getNetAmount()));
				line(text, regular, 10, 330, y - 16, "TVA " + invoice.getVatRate().stripTrailingZeros().toPlainString() + " %");
				line(text, regular, 10, 460, y - 16, euros(invoice.getVatAmount()));
				line(text, bold, 11, 330, y - 34, "Total TTC");
				line(text, bold, 11, 460, y - 34, euros(invoice.getGrossAmount()));
				line(text, regular, 10, 330, y - 52, "Deja paye (carte, Stripe)");
				line(text, regular, 10, 460, y - 52, euros(invoice.getGrossAmount()));
				line(text, bold, 10, 330, y - 70, "Reste a payer");
				line(text, bold, 10, 460, y - 70, euros(BigDecimal.ZERO));

				line(text, regular, 8, LEFT, 70, "Facture electronique au format Peppol BIS Billing 3.0 (EN 16931). "
						+ "Le fichier XML structure fait foi entre entreprises.");
			}
			document.save(output);
			return output.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("Le PDF de la facture n'a pas pu etre produit.", exception);
		}
	}

	private void line(PDPageContentStream text, PDType1Font font, float size, float x, float y, String value) throws IOException {
		text.beginText();
		text.setFont(font, size);
		text.newLineAtOffset(x, y);
		text.showText(winAnsi(value));
		text.endText();
	}

	/** Les polices standard du PDF ne couvrent que l'alphabet latin courant : les autres lettres perdent leur accent. */
	private String winAnsi(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder output = new StringBuilder();
		for (char character : value.toCharArray()) {
			if (character <= 0xFF) {
				output.append(character);
			} else {
				String base = Normalizer.normalize(String.valueOf(character), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
				output.append(!base.isEmpty() && base.charAt(0) <= 0xFF ? base.charAt(0) : '?');
			}
		}
		return output.toString();
	}

	private String clip(String value, int length) {
		if (value == null) {
			return "";
		}
		return value.length() <= length ? value : value.substring(0, length - 1) + ".";
	}

	private String euros(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " EUR";
	}
}

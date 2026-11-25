package be.icc.metamind.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class EnterpriseNumberTests {
	@Test
	void acceptsBelgianNumbersWrittenInEveryUsualWay() {
		assertThat(EnterpriseNumber.isValid("0123456749")).isTrue();
		assertThat(EnterpriseNumber.isValid("BE 0123.456.749")).isTrue();
		assertThat(EnterpriseNumber.isValid("123.456.749")).isTrue();
		assertThat(EnterpriseNumber.normalize("BE0123.456.749")).isEqualTo("0123456749");
		assertThat(EnterpriseNumber.format("0123456749")).isEqualTo("0123.456.749");
	}

	@Test
	void rejectsAWrongCheckDigitOrLength() {
		assertThat(EnterpriseNumber.isValid("0123456748")).isFalse();
		assertThat(EnterpriseNumber.isValid("01234567")).isFalse();
		assertThat(EnterpriseNumber.isValid("2123456749")).isFalse();
		assertThat(EnterpriseNumber.isValid(null)).isFalse();
	}

	@Test
	void netAmountAndVatAddUpToThePaidPriceToTheCent() {
		for (String paid : new String[] {"50.00", "200.00", "0.01", "9.99", "121.00", "1234.56"}) {
			BigDecimal gross = new BigDecimal(paid);
			BigDecimal net = InvoiceService.netAmountFor(gross);
			BigDecimal vat = net.multiply(InvoiceService.VAT_RATE).movePointLeft(2).setScale(2, java.math.RoundingMode.HALF_UP);
			assertThat(net.add(vat)).as(paid).isEqualByComparingTo(gross);
		}
		assertThat(InvoiceService.netAmountFor(new BigDecimal("50.00"))).isEqualByComparingTo("41.32");
	}
}

package be.icc.metamind.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import be.icc.metamind.document.CreditPackEntity;
import be.icc.metamind.institution.InstitutionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Facture d'un achat de credits. Les montants et les donnees de l'acheteur sont figes a l'emission :
 * une facture emise ne change plus, meme si l'institution modifie ensuite ses informations.
 */
@Entity
@Table(name = "factures")
public class InvoiceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "numero", length = 20)
	private String number;

	@Column(name = "annee")
	private Integer year;

	@Column(name = "sequence")
	private Integer sequence;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "institution_id", nullable = false)
	private InstitutionEntity institution;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pack_id", nullable = false)
	private CreditPackEntity pack;

	@Column(name = "date_emission", nullable = false)
	private LocalDate issueDate;

	@Column(name = "description", nullable = false, length = 255)
	private String description;

	@Column(name = "quantite", nullable = false)
	private int quantity;

	@Column(name = "montant_ht", nullable = false, precision = 10, scale = 2)
	private BigDecimal netAmount;

	@Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
	private BigDecimal vatRate;

	@Column(name = "montant_tva", nullable = false, precision = 10, scale = 2)
	private BigDecimal vatAmount;

	@Column(name = "montant_ttc", nullable = false, precision = 10, scale = 2)
	private BigDecimal grossAmount;

	@Column(name = "devise", nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private InvoiceStatus status;

	@Column(name = "acheteur_nom", length = 255)
	private String buyerName;

	@Column(name = "acheteur_numero_entreprise", length = 20)
	private String buyerEnterpriseNumber;

	@Column(name = "acheteur_adresse_rue", length = 255)
	private String buyerStreet;

	@Column(name = "acheteur_code_postal", length = 20)
	private String buyerPostalCode;

	@Column(name = "acheteur_ville", length = 120)
	private String buyerCity;

	@Column(name = "acheteur_pays", length = 2)
	private String buyerCountry;

	@Column(name = "xml_ubl", columnDefinition = "text")
	private String ublXml;

	@Column(name = "date_creation", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected InvoiceEntity() {
	}

	public InvoiceEntity(CreditPackEntity pack, LocalDate issueDate, String description, BigDecimal netAmount, BigDecimal vatRate,
			BigDecimal vatAmount, BigDecimal grossAmount, String currency) {
		this.pack = pack;
		this.institution = pack.getInstitution();
		this.issueDate = issueDate;
		this.description = description;
		this.quantity = 1;
		this.netAmount = netAmount;
		this.vatRate = vatRate;
		this.vatAmount = vatAmount;
		this.grossAmount = grossAmount;
		this.currency = currency;
		this.status = InvoiceStatus.A_COMPLETER;
	}

	/** Attribue le numero et fige l'acheteur : la facture est alors emise et ne change plus. */
	void markIssued(int year, int sequence, LocalDate issueDate, InstitutionEntity buyer) {
		this.year = year;
		this.sequence = sequence;
		this.number = "%d-%04d".formatted(year, sequence);
		this.issueDate = issueDate;
		this.buyerName = buyer.getLegalName();
		this.buyerEnterpriseNumber = buyer.getEnterpriseNumber();
		this.buyerStreet = buyer.getStreet();
		this.buyerPostalCode = buyer.getPostalCode();
		this.buyerCity = buyer.getCity();
		this.buyerCountry = buyer.getCountry();
		this.status = InvoiceStatus.EMISE;
	}

	void attachXml(String ublXml) {
		this.ublXml = ublXml;
	}

	public Long getId() {
		return id;
	}

	public String getNumber() {
		return number;
	}

	public InstitutionEntity getInstitution() {
		return institution;
	}

	public CreditPackEntity getPack() {
		return pack;
	}

	public LocalDate getIssueDate() {
		return issueDate;
	}

	public String getDescription() {
		return description;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getNetAmount() {
		return netAmount;
	}

	public BigDecimal getVatRate() {
		return vatRate;
	}

	public BigDecimal getVatAmount() {
		return vatAmount;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public InvoiceStatus getStatus() {
		return status;
	}

	public String getBuyerName() {
		return buyerName;
	}

	public String getBuyerEnterpriseNumber() {
		return buyerEnterpriseNumber;
	}

	public String getBuyerStreet() {
		return buyerStreet;
	}

	public String getBuyerPostalCode() {
		return buyerPostalCode;
	}

	public String getBuyerCity() {
		return buyerCity;
	}

	public String getBuyerCountry() {
		return buyerCountry;
	}

	public String getUblXml() {
		return ublXml;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}

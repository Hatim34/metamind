package be.icc.metamind.invoice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Identite du vendeur imprimee sur chaque facture. Tant que Metamind n'a pas de numero d'entreprise
 * (projet academique), des valeurs de demonstration sont utilisees ; elles se remplacent par
 * les variables d'environnement METAMIND_VENDEUR_*, sans toucher au code.
 */
@Component
public class SellerSettings {
	private final String name;
	private final String enterpriseNumber;
	private final String street;
	private final String postalCode;
	private final String city;
	private final String country;
	private final String email;
	private final boolean demonstration;

	public SellerSettings(
			@Value("${metamind.vendeur.nom:Metamind (projet academique ICC, donnees de demonstration)}") String name,
			@Value("${metamind.vendeur.numero-entreprise:0123456749}") String enterpriseNumber,
			@Value("${metamind.vendeur.rue:Rue de la Demonstration 1}") String street,
			@Value("${metamind.vendeur.code-postal:1000}") String postalCode,
			@Value("${metamind.vendeur.ville:Bruxelles}") String city,
			@Value("${metamind.vendeur.pays:BE}") String country,
			@Value("${metamind.vendeur.email:facturation@metamind.example}") String email,
			@Value("${metamind.vendeur.demonstration:true}") boolean demonstration
	) {
		this.name = name;
		this.enterpriseNumber = EnterpriseNumber.normalize(enterpriseNumber);
		this.street = street;
		this.postalCode = postalCode;
		this.city = city;
		this.country = country;
		this.email = email;
		this.demonstration = demonstration;
	}

	public String name() {
		return name;
	}

	public String enterpriseNumber() {
		return enterpriseNumber;
	}

	public String street() {
		return street;
	}

	public String postalCode() {
		return postalCode;
	}

	public String city() {
		return city;
	}

	public String country() {
		return country;
	}

	public String email() {
		return email;
	}

	/** Les factures portent alors la mention « sans valeur legale ». */
	public boolean demonstration() {
		return demonstration;
	}
}

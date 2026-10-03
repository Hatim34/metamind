package be.icc.metamind.credit;

public enum CreditMovementType {
	ACHAT("Achat de credits"),
	CONSOMMATION("Extraction de metadonnees"),
	REMBOURSEMENT("Remboursement de credits"),
	AJUSTEMENT_ADMIN("Ajustement administratif"),
	OFFRE_BIENVENUE("Offre de bienvenue");

	private final String defaultDescription;

	CreditMovementType(String defaultDescription) {
		this.defaultDescription = defaultDescription;
	}

	public String defaultDescription() {
		return defaultDescription;
	}
}

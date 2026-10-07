package be.icc.metamind.notification;

/**
 * Textes des emails, en francais, neerlandais et anglais selon la langue du compte.
 * Aucun mot de passe n'est jamais ecrit dans un email.
 */
public final class MailTemplates {
	public record Mail(String subject, String body) {
	}

	private MailTemplates() {
	}

	public static Mail requestReceived(String language, String firstName, String institution, boolean newInstitution) {
		return switch (language) {
			case "nl" -> new Mail("Metamind: uw aanvraag is ontvangen",
					"Beste " + firstName + ",\n\n"
							+ (newInstitution
									? "Uw aanvraag om " + institution + " aan Metamind toe te voegen is ontvangen. De beheerder valideert eerst de instelling en daarna uw account.\n\n"
									: "Uw accountaanvraag voor " + institution + " is ontvangen. Een beheerder moet ze valideren.\n\n")
							+ "U krijgt een e-mail zodra uw account actief is.\n\nMetamind");
			case "en" -> new Mail("Metamind: your request has been received",
					"Hello " + firstName + ",\n\n"
							+ (newInstitution
									? "Your request to add " + institution + " to Metamind has been received. The administrator will first validate the institution, then your account.\n\n"
									: "Your account request for " + institution + " has been received. An administrator must validate it.\n\n")
							+ "You will receive an email as soon as your account is active.\n\nMetamind");
			default -> new Mail("Metamind : votre demande a bien été reçue",
					"Bonjour " + firstName + ",\n\n"
							+ (newInstitution
									? "Votre demande d'ajout de " + institution + " à Metamind a bien été reçue. L'administrateur validera d'abord l'institution, puis votre compte.\n\n"
									: "Votre demande de compte pour " + institution + " a bien été reçue. Un administrateur doit la valider.\n\n")
							+ "Vous recevrez un email dès que votre compte sera actif.\n\nMetamind");
		};
	}

	/** Toujours en francais : les administrateurs de la plateforme travaillent dans la langue de l'application. */
	public static Mail newRequestForAdmin(String fullName, String email, String institution, boolean newInstitution, String adminUrl) {
		return new Mail(newInstitution ? "Metamind : nouvelle demande d'institution" : "Metamind : nouvelle demande de compte",
				(newInstitution
						? "Une personne demande l'ajout d'une institution qui n'est pas encore inscrite.\n\n"
						: "Une nouvelle demande de compte attend votre décision.\n\n")
						+ "Nom : " + fullName + "\n"
						+ "Adresse : " + email + "\n"
						+ (newInstitution ? "Institution demandée : " : "Institution : ") + institution + "\n\n"
						+ "Pour valider ou refuser : " + adminUrl + "\n\nMetamind");
	}

	public static Mail accountActivated(String language, String firstName, String loginUrl) {
		return switch (language) {
			case "nl" -> new Mail("Metamind: uw account is actief",
					"Beste " + firstName + ",\n\nUw account is gevalideerd. U kunt nu inloggen met uw e-mailadres en wachtwoord:\n" + loginUrl + "\n\nMetamind");
			case "en" -> new Mail("Metamind: your account is active",
					"Hello " + firstName + ",\n\nYour account has been validated. You can now sign in with your email address and password:\n" + loginUrl + "\n\nMetamind");
			default -> new Mail("Metamind : votre compte est actif",
					"Bonjour " + firstName + ",\n\nVotre compte a été validé. Vous pouvez maintenant vous connecter avec votre adresse email et votre mot de passe :\n" + loginUrl + "\n\nMetamind");
		};
	}

	public static Mail requestRefused(String language, String firstName) {
		return switch (language) {
			case "nl" -> new Mail("Metamind: uw aanvraag is geweigerd",
					"Beste " + firstName + ",\n\nUw accountaanvraag is niet aanvaard. Neem contact op met uw bibliotheek of met de beheerder van het platform als u denkt dat dit een vergissing is.\n\nMetamind");
			case "en" -> new Mail("Metamind: your request was declined",
					"Hello " + firstName + ",\n\nYour account request was not accepted. Contact your library or the platform administrator if you think this is a mistake.\n\nMetamind");
			default -> new Mail("Metamind : votre demande n'a pas été acceptée",
					"Bonjour " + firstName + ",\n\nVotre demande de compte n'a pas été acceptée. Contactez votre bibliothèque ou l'administrateur de la plateforme si vous pensez qu'il s'agit d'une erreur.\n\nMetamind");
		};
	}

	public static Mail accountDeactivated(String language, String firstName) {
		return switch (language) {
			case "nl" -> new Mail("Metamind: uw account is gedeactiveerd",
					"Beste " + firstName + ",\n\nUw Metamind-account is gedeactiveerd door een beheerder. U kunt niet meer inloggen. Neem contact op met de beheerder als u denkt dat dit een vergissing is.\n\nMetamind");
			case "en" -> new Mail("Metamind: your account has been deactivated",
					"Hello " + firstName + ",\n\nYour Metamind account has been deactivated by an administrator. You can no longer sign in. Contact the administrator if you think this is a mistake.\n\nMetamind");
			default -> new Mail("Metamind : votre compte a été désactivé",
					"Bonjour " + firstName + ",\n\nVotre compte Metamind a été désactivé par un administrateur. Vous ne pouvez plus vous connecter. Contactez l'administrateur si vous pensez qu'il s'agit d'une erreur.\n\nMetamind");
		};
	}

	public static Mail passwordReset(String language, String firstName, String resetUrl, long minutes) {
		return switch (language) {
			case "nl" -> new Mail("Metamind: nieuw wachtwoord kiezen",
					"Beste " + firstName + ",\n\nU hebt een nieuw wachtwoord aangevraagd. Deze link is " + minutes + " minuten geldig en kan maar één keer gebruikt worden:\n" + resetUrl
							+ "\n\nHebt u niets aangevraagd? Negeer dan deze e-mail: uw wachtwoord blijft ongewijzigd.\n\nMetamind");
			case "en" -> new Mail("Metamind: choose a new password",
					"Hello " + firstName + ",\n\nYou asked for a new password. This link is valid for " + minutes + " minutes and can only be used once:\n" + resetUrl
							+ "\n\nDid not ask for anything? Ignore this email: your password stays unchanged.\n\nMetamind");
			default -> new Mail("Metamind : choisir un nouveau mot de passe",
					"Bonjour " + firstName + ",\n\nVous avez demandé un nouveau mot de passe. Ce lien est valable " + minutes + " minutes et ne peut servir qu'une fois :\n" + resetUrl
							+ "\n\nVous n'avez rien demandé ? Ignorez cet email : votre mot de passe reste inchangé.\n\nMetamind");
		};
	}
}

package be.icc.metamind.notification;

import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;
import be.icc.metamind.user.UserStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transforme les evenements de compte en emails.
 *
 * Traite apres la validation de la transaction et en arriere-plan : la reponse a
 * l'utilisateur ne depend ni de la lenteur du serveur SMTP, ni de l'existence du compte
 * (sinon le temps de reponse de « mot de passe oublie » trahirait les adresses inscrites).
 */
@Component
public class AccountNotifications {
	private final UserRepository userRepository;
	private final MailService mailService;
	private final String publicUrl;

	public AccountNotifications(UserRepository userRepository, MailService mailService,
			@Value("${metamind.public-url:https://metamind-app.duckdns.org}") String publicUrl) {
		this.userRepository = userRepository;
		this.mailService = mailService;
		this.publicUrl = publicUrl.replaceAll("/$", "");
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void on(AccountEvent event) {
		UserEntity user = userRepository.findById(event.userId()).orElse(null);
		if (user == null) {
			return;
		}
		String language = user.getPreferredLanguage();
		switch (event) {
			case AccountEvent.Requested requested -> {
				boolean newInstitution = requested.requestedInstitution() != null;
				String institution = newInstitution ? requested.requestedInstitution() : user.getInstitution().getName();
				mailService.send(user.getEmail(), MailTemplates.requestReceived(language, user.getFirstName(), institution, newInstitution));
				MailTemplates.Mail forAdmin = MailTemplates.newRequestForAdmin(
						user.getFirstName() + " " + user.getLastName(), user.getEmail(), institution, newInstitution, publicUrl + "/admin");
				userRepository.findAll().stream()
						.filter(admin -> admin.getRole() == UserRole.ADMIN && admin.getStatus() == UserStatus.ACTIF)
						.forEach(admin -> mailService.send(admin.getEmail(), forAdmin));
			}
			case AccountEvent.StatusChanged changed -> {
				if (changed.current() == UserStatus.ACTIF && changed.previous() != UserStatus.ACTIF) {
					mailService.send(user.getEmail(), MailTemplates.accountActivated(language, user.getFirstName(), publicUrl + "/connexion"));
				} else if (changed.current() == UserStatus.DESACTIVE && changed.previous() == UserStatus.EN_ATTENTE) {
					mailService.send(user.getEmail(), MailTemplates.requestRefused(language, user.getFirstName()));
				} else if (changed.current() == UserStatus.DESACTIVE && changed.previous() == UserStatus.ACTIF) {
					mailService.send(user.getEmail(), MailTemplates.accountDeactivated(language, user.getFirstName()));
				}
			}
			case AccountEvent.PasswordResetRequested reset -> mailService.send(user.getEmail(), MailTemplates.passwordReset(
					language, user.getFirstName(), publicUrl + "/reset-password?token=" + reset.token(), reset.expirationMinutes()));
		}
	}
}

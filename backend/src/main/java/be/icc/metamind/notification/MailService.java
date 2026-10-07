package be.icc.metamind.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi des emails de la plateforme.
 *
 * Sans serveur SMTP configure (demonstration locale), le message est ecrit dans les journaux
 * pour pouvoir suivre le parcours. Avec un serveur configure, rien de sensible n'est journalise :
 * un lien de reinitialisation dans les journaux permettrait a quiconque les lit de prendre le compte.
 */
@Service
public class MailService {
	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final String smtpHost;
	private final String mailFrom;

	public MailService(
			ObjectProvider<JavaMailSender> mailSenderProvider,
			@Value("${spring.mail.host:}") String smtpHost,
			@Value("${metamind.mail.from:}") String mailFrom
	) {
		this.mailSenderProvider = mailSenderProvider;
		this.smtpHost = smtpHost == null ? "" : smtpHost.trim();
		this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
	}

	public boolean isConfigured() {
		return !smtpHost.isEmpty() && mailSenderProvider.getIfAvailable() != null;
	}

	public void send(String to, MailTemplates.Mail mail) {
		if (!isConfigured()) {
			log.info("Email non envoye (aucun serveur SMTP configure) a {} : {}\n{}", to, mail.subject(), mail.body());
			return;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(to);
			if (!mailFrom.isEmpty()) {
				message.setFrom(mailFrom);
			}
			message.setSubject(mail.subject());
			message.setText(mail.body());
			mailSenderProvider.getObject().send(message);
		}
		catch (RuntimeException exception) {
			log.warn("Envoi de l'email \"{}\" a {} impossible : {}", mail.subject(), to, exception.getMessage());
		}
	}
}

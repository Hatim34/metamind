package be.icc.metamind.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import be.icc.metamind.institution.InstitutionEntity;
import be.icc.metamind.user.UserEntity;
import be.icc.metamind.user.UserRepository;
import be.icc.metamind.user.UserRole;
import be.icc.metamind.user.UserStatus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AccountNotificationsTests {
	private final UserRepository users = mock(UserRepository.class);
	private final MailService mail = mock(MailService.class);
	private final AccountNotifications notifications = new AccountNotifications(users, mail, "https://metamind.test/");

	private UserEntity user(String email, UserRole role, UserStatus status, String language) {
		UserEntity user = new UserEntity("Lea", "Dumont", email, "hash", role, new InstitutionEntity("ULB", "ULB", "ulb.be"));
		user.updateAdministration(null, status);
		user.choosePreferredLanguage(language);
		return user;
	}

	@Test
	void anActivatedAccountIsToldInItsOwnLanguage() {
		when(users.findById(7L)).thenReturn(Optional.of(user("lea@ulb.be", UserRole.LIBRARIAN, UserStatus.ACTIF, "nl")));

		notifications.on(new AccountEvent.StatusChanged(7L, UserStatus.EN_ATTENTE, UserStatus.ACTIF));

		ArgumentCaptor<MailTemplates.Mail> sent = ArgumentCaptor.forClass(MailTemplates.Mail.class);
		verify(mail).send(eq("lea@ulb.be"), sent.capture());
		assertThat(sent.getValue().subject()).contains("actief");
		assertThat(sent.getValue().body()).contains("https://metamind.test/connexion");
	}

	@Test
	void aRefusedRequestIsNotToldItWasDeactivated() {
		when(users.findById(7L)).thenReturn(Optional.of(user("lea@ulb.be", UserRole.LIBRARIAN, UserStatus.DESACTIVE, "fr")));

		notifications.on(new AccountEvent.StatusChanged(7L, UserStatus.EN_ATTENTE, UserStatus.DESACTIVE));

		ArgumentCaptor<MailTemplates.Mail> sent = ArgumentCaptor.forClass(MailTemplates.Mail.class);
		verify(mail).send(eq("lea@ulb.be"), sent.capture());
		assertThat(sent.getValue().subject()).contains("n'a pas été acceptée");
	}

	@Test
	void aNewRequestReachesEveryActiveAdministratorButNoOneElse() {
		UserEntity requester = user("lea@ulb.be", UserRole.LIBRARIAN, UserStatus.EN_ATTENTE, "fr");
		when(users.findById(7L)).thenReturn(Optional.of(requester));
		when(users.findAll()).thenReturn(List.of(
				requester,
				user("admin@metamind.test", UserRole.ADMIN, UserStatus.ACTIF, "fr"),
				user("ancien@metamind.test", UserRole.ADMIN, UserStatus.DESACTIVE, "fr")));

		notifications.on(new AccountEvent.Requested(7L, null));

		verify(mail).send(eq("lea@ulb.be"), any());
		verify(mail).send(eq("admin@metamind.test"), any());
		verify(mail, never()).send(eq("ancien@metamind.test"), any());
	}

	@Test
	void theResetLinkPointsToThePublicSiteAndStatesItsLifetime() {
		when(users.findById(7L)).thenReturn(Optional.of(user("lea@ulb.be", UserRole.LIBRARIAN, UserStatus.ACTIF, "en")));

		notifications.on(new AccountEvent.PasswordResetRequested(7L, "abc123", 30));

		ArgumentCaptor<MailTemplates.Mail> sent = ArgumentCaptor.forClass(MailTemplates.Mail.class);
		verify(mail).send(eq("lea@ulb.be"), sent.capture());
		assertThat(sent.getValue().body()).contains("https://metamind.test/reset-password?token=abc123").contains("30 minutes");
	}
}

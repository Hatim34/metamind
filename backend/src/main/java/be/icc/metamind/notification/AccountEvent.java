package be.icc.metamind.notification;

import be.icc.metamind.user.UserStatus;

/**
 * Evenements de compte qui donnent lieu a un email. Ils sont traites apres la validation
 * de la transaction : un email ne part jamais pour une action annulee.
 */
public sealed interface AccountEvent {
	long userId();

	/** Inscription enregistree, en attente de l'administrateur. */
	record Requested(long userId, String requestedInstitution) implements AccountEvent {
	}

	/** Statut change par un administrateur (activation, refus, desactivation). */
	record StatusChanged(long userId, UserStatus previous, UserStatus current) implements AccountEvent {
	}

	/** Lien de reinitialisation : le jeton n'existe en clair que dans cet evenement et l'email. */
	record PasswordResetRequested(long userId, String token, long expirationMinutes) implements AccountEvent {
	}
}

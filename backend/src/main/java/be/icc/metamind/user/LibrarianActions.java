package be.icc.metamind.user;

import be.icc.metamind.api.ApiException;

import org.springframework.http.HttpStatus;

/**
 * Actions reservees au bibliothecaire (cahier des charges B3, B5, B6) : importer, lancer
 * l'extraction, valider ou rejeter une notice. L'administrateur supervise la plateforme
 * (comptes, institutions, credits, configuration, journaux) mais ne catalogue pas.
 */
public final class LibrarianActions {
	private LibrarianActions() {
	}

	public static void require(UserEntity user, String action) {
		if (user.getRole() == UserRole.ADMIN) {
			throw new ApiException(HttpStatus.FORBIDDEN,
					"Seul un bibliothecaire de l'institution peut " + action + ". L'administrateur supervise sans cataloguer.");
		}
	}
}

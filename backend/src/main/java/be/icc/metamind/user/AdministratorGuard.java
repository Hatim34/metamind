package be.icc.metamind.user;

import be.icc.metamind.api.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Garantit qu'il reste toujours au moins un administrateur actif.
 *
 * Trois operations pouvaient sinon rendre la plateforme iningerable : supprimer son
 * propre compte, se retrograder en bibliothecaire, se desactiver. Plus aucun administrateur
 * signifie plus personne pour activer les comptes en attente, gerer les institutions,
 * ajuster les credits ni consulter les journaux : une situation irreversible depuis l'interface.
 */
@Service
public class AdministratorGuard {
	private final UserRepository userRepository;

	public AdministratorGuard(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Verifie que l'utilisateur vise peut perdre ses droits d'administration.
	 * Ne refuse que si c'est le dernier administrateur actif.
	 */
	public void ensureAnotherActiveAdministratorRemains(UserEntity user, String action) {
		if (user.getRole() != UserRole.ADMIN || user.getStatus() != UserStatus.ACTIF) {
			return;
		}
		long autres = userRepository.countByRoleAndStatusAndIdNot(UserRole.ADMIN, UserStatus.ACTIF, user.getId());
		if (autres == 0) {
			throw new ApiException(HttpStatus.CONFLICT,
					"Impossible de " + action + " : c'est le dernier administrateur actif. "
							+ "Nommez d'abord un autre administrateur.");
		}
	}

	/** L'operation retire-t-elle les droits d'administration (retrogradation ou desactivation) ? */
	public boolean removesAdministration(UserRole newRole, UserStatus newStatus) {
		return (newRole != null && newRole != UserRole.ADMIN)
				|| (newStatus != null && newStatus != UserStatus.ACTIF);
	}
}

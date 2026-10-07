import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService);
  return session.isLoggedIn() ? true : inject(Router).createUrlTree(['/connexion'], { queryParams: { retour: state.url } });
};

export const adminGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  if (session.isAdmin()) {
    return true;
  }
  return inject(Router).createUrlTree([session.isLoggedIn() ? '/espace' : '/connexion']);
};

/** Importer et valider sont réservés au bibliothécaire (B3, B6) : l'administrateur supervise. */
export const librarianGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  return session.isAdmin() ? inject(Router).createUrlTree(['/espace/file']) : true;
};

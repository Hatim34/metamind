import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFrBe from '@angular/common/locales/fr-BE';

registerLocaleData(localeFrBe);
import { provideHttpClient } from '@angular/common/http';
import { NavigationError, provideRouter, withComponentInputBinding, withInMemoryScrolling, withNavigationErrorHandler } from '@angular/router';

import { routes } from './app.routes';

/**
 * Après un déploiement, un onglet resté ouvert réclame des morceaux de l'application qui n'existent plus.
 * On recharge alors la page demandée une fois, pour récupérer la nouvelle version.
 */
function reloadAfterDeployment(event: NavigationError): void {
  const message = String((event.error as Error | undefined)?.message ?? event.error ?? '');
  if (!/dynamically imported module|Importing a module script failed|Loading chunk/i.test(message)) return;
  const key = 'metamind.rechargement';
  try {
    if (sessionStorage.getItem(key) === event.url) return;
    sessionStorage.setItem(key, event.url);
  } catch { /* stockage indisponible : on recharge quand même */ }
  window.location.assign(event.url);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(),
    { provide: LOCALE_ID, useValue: 'fr-BE' }, // nombres et dates au format belge (5,4 h ; 0,40 €)
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'top' }),
      withNavigationErrorHandler(reloadAfterDeployment))
  ]
};

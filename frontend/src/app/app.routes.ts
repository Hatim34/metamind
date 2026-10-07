import { Routes } from '@angular/router';
import { adminGuard, authGuard, librarianGuard } from './core/guards';

const pub = { layout: 'public' };
const espace = { layout: 'espace' };

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/accueil.page').then((m) => m.AccueilPage), data: pub, title: 'Metamind' },
  { path: 'catalogue', loadComponent: () => import('./pages/catalogue.page').then((m) => m.CataloguePage), data: pub, title: 'Catalogue – Metamind' },
  { path: 'publications/:id', loadComponent: () => import('./pages/fiche.page').then((m) => m.FichePage), data: pub, title: 'Publication – Metamind' },
  { path: 'legal', loadComponent: () => import('./pages/legal.page').then((m) => m.LegalPage), data: pub, title: 'Informations légales – Metamind' },

  { path: 'connexion', loadComponent: () => import('./pages/auth.page').then((m) => m.AuthPage), data: { layout: 'bare', mode: 'connexion' }, title: 'Connexion – Metamind' },
  { path: 'inscription', loadComponent: () => import('./pages/auth.page').then((m) => m.AuthPage), data: { layout: 'bare', mode: 'inscription' }, title: 'Demander un accès – Metamind' },
  { path: 'mot-de-passe', loadComponent: () => import('./pages/auth.page').then((m) => m.AuthPage), data: { layout: 'bare', mode: 'oubli' }, title: 'Mot de passe oublié – Metamind' },
  { path: 'reset-password', loadComponent: () => import('./pages/auth.page').then((m) => m.AuthPage), data: { layout: 'bare', mode: 'reinitialiser' }, title: 'Nouveau mot de passe – Metamind' },

  { path: 'espace', canActivate: [authGuard], data: espace, children: [
    { path: '', loadComponent: () => import('./pages/espace.page').then((m) => m.EspacePage), data: espace, title: 'Tableau de bord – Metamind' },
    { path: 'import', canActivate: [librarianGuard], loadComponent: () => import('./pages/import.page').then((m) => m.ImportPage), data: espace, title: 'Importer – Metamind' },
    { path: 'file', loadComponent: () => import('./pages/file.page').then((m) => m.FilePage), data: espace, title: 'File de validation – Metamind' },
    { path: 'documents/:id/validation', canActivate: [librarianGuard], loadComponent: () => import('./pages/validation.page').then((m) => m.ValidationPage), data: espace, title: 'Valider les métadonnées – Metamind' },
    { path: 'credits', loadComponent: () => import('./pages/credits.page').then((m) => m.CreditsPage), data: espace, title: 'Crédits – Metamind' },
    { path: 'profil', loadComponent: () => import('./pages/profil.page').then((m) => m.ProfilPage), data: espace, title: 'Profil – Metamind' }
  ] },
  // Adresses de retour utilisées par le backend actuel (Stripe, ancien lien)
  { path: 'paiement/succes', canActivate: [authGuard], loadComponent: () => import('./pages/credits.page').then((m) => m.CreditsPage), data: { ...espace, retourPaiement: true } },
  { path: 'paiement/confirmation', redirectTo: 'espace/credits' },
  { path: 'credits', redirectTo: 'espace/credits' },

  { path: 'admin', canActivate: [adminGuard], loadComponent: () => import('./pages/admin.page').then((m) => m.AdminPage), data: { layout: 'admin' }, title: 'Administration – Metamind' },
  { path: '**', loadComponent: () => import('./pages/legal.page').then((m) => m.NotFoundPage), data: pub, title: 'Page introuvable – Metamind' }
];

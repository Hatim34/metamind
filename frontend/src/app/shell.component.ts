import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { SessionService } from './core/session.service';
import { I18nService, Language, TranslatePipe } from './core/i18n';
import { ApiService } from './api.service';
import { ToastService } from './core/toast.service';

type Layout = 'public' | 'espace' | 'admin' | 'bare';

/** Coquille de l'application : en-tête selon la zone, contenu routé, pied de page, notifications. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  template: `
    <a class="m-skip" href="#contenu">{{ 'Aller au contenu' | t }}</a>
    @if (layout() !== 'bare') {
      <header class="m-header" [class.m-header--admin]="layout() === 'admin'">
        <div class="m-header__in" [class.m-wrap--public]="layout() === 'public'">
          <a class="m-logo" [routerLink]="session.isLoggedIn() && layout() !== 'public' ? session.homeUrl() : '/'">
            <span class="m-logo__mark" aria-hidden="true">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round"><circle cx="6" cy="6" r="2.2"/><circle cx="18" cy="6" r="2.2"/><circle cx="12" cy="18" r="2.2"/><path d="M7.6 7.6 11 16M16.4 7.6 13 16M8.2 6h7.6"/></svg>
            </span>
            <span class="m-logo__name">Metamind</span>
            @if (layout() === 'admin') { <span class="m-logo__tag">{{ 'Administration' | t }}</span> }
          </a>

          @if (layout() === 'public') {
            <nav class="m-nav" [attr.aria-label]="'Navigation principale' | t">
              <a routerLink="/catalogue" routerLinkActive="is-active">{{ 'Catalogue' | t }}</a>
              <a routerLink="/legal">{{ 'À propos des données' | t }}</a>
            </nav>
          }
          @if (layout() === 'espace' || layout() === 'admin') {
            <nav class="m-nav" [attr.aria-label]="'Espace' | t">
              <a routerLink="/espace" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">{{ 'Tableau de bord' | t }}</a>
              <a routerLink="/espace/import" routerLinkActive="is-active">{{ 'Importer' | t }}</a>
              <a routerLink="/espace/file" routerLinkActive="is-active">{{ 'File de validation' | t }}</a>
              <a routerLink="/espace/credits" routerLinkActive="is-active">{{ 'Crédits' | t }}</a>
              <a routerLink="/catalogue">{{ 'Catalogue' | t }}</a>
              @if (session.isAdmin()) { <a routerLink="/admin">{{ 'Administration' | t }}</a> }
            </nav>
          }

          <div class="m-header__end">
            <span class="m-langs" role="group" [attr.aria-label]="'Langue' | t">
              @for (lang of languages; track lang) {
                <button type="button" [class.is-active]="i18n.lang() === lang" [attr.aria-pressed]="i18n.lang() === lang" (click)="i18n.setLanguage(lang)">{{ lang.toUpperCase() }}</button>
              }
            </span>
            @if (session.user(); as user) {
              @if ((layout() === 'espace' || layout() === 'admin') && credits() !== null) {
                <a class="m-header__credits" routerLink="/espace/credits">{{ 'Crédits' | t }} <strong>{{ credits() }}</strong></a>
              }
              <details class="m-menu">
                <summary class="m-avatar" [attr.aria-label]="user.firstName + ' ' + user.lastName">{{ initials() }}</summary>
                <div class="m-menu__panel">
                  <span class="m-menu__who">{{ user.firstName }} {{ user.lastName }}<br /><small>{{ user.institution }}</small></span>
                  <a routerLink="/espace">{{ 'Espace bibliothécaire' | t }}</a>
                  <a routerLink="/espace/profil">{{ 'Profil' | t }}</a>
                  <button type="button" (click)="session.logout()">{{ 'Se déconnecter' | t }}</button>
                </div>
              </details>
            } @else {
              <a class="m-btn m-btn--ghost" routerLink="/connexion">{{ 'Espace bibliothécaire' | t }}</a>
            }
          </div>
        </div>
      </header>
    }

    <main id="contenu" tabindex="-1">
      <router-outlet />
    </main>

    @if (layout() === 'public') {
      <footer class="m-footer">
        <div class="m-wrap m-wrap--public m-footer__in">
          <span>{{ 'Métadonnées proposées par IA et vérifiées par les bibliothécaires.' | t }} Metamind · ICC 2025-2026</span>
          <nav [attr.aria-label]="'Informations légales' | t">
            <a routerLink="/legal" fragment="mentions-legales">{{ 'Mentions légales' | t }}</a>
            <a routerLink="/legal" fragment="confidentialite">{{ 'Confidentialité' | t }}</a>
            <a routerLink="/legal" fragment="cgv">{{ 'CGV' | t }}</a>
            <a routerLink="/legal" fragment="ia">{{ 'Comment nous utilisons l\\'IA' | t }}</a>
          </nav>
        </div>
      </footer>
    }

    <div class="m-toasts" aria-live="polite">
      @for (toast of toasts.items(); track toast.id) {
        <div class="m-toast" [class.m-toast--error]="toast.kind === 'error'" role="status">
          <span>{{ toast.text }}</span>
          <button type="button" (click)="toasts.dismiss(toast.id)" [attr.aria-label]="'Fermer' | t">×</button>
        </div>
      }
    </div>
  `
})
export class ShellComponent {
  readonly session = inject(SessionService);
  readonly i18n = inject(I18nService);
  readonly toasts = inject(ToastService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly languages: Language[] = ['fr', 'nl', 'en'];
  readonly layout = signal<Layout>('public');
  readonly credits = signal<number | null>(null);
  readonly initials = computed(() => {
    const user = this.session.user();
    return user ? (user.firstName[0] ?? '') + (user.lastName[0] ?? '') : '';
  });

  constructor() {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      let current = this.route;
      while (current.firstChild) {
        current = current.firstChild;
      }
      this.layout.set((current.snapshot.data['layout'] as Layout) ?? 'public');
      if (this.session.isLoggedIn() && this.layout() === 'espace') {
        this.api.getCreditAccount().subscribe({ next: (account) => this.credits.set(account.balance.balance), error: () => this.credits.set(null) });
      }
    });
  }
}

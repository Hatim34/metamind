import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { I18nService, Language, TranslatePipe } from '../core/i18n';
import { SessionService } from '../core/session.service';

type Mode = 'connexion' | 'inscription' | 'oubli' | 'reinitialiser';

/** Connexion (B2), inscription (B1), mot de passe oublié et réinitialisation. */
const PERSONAL_EMAIL_DOMAINS = ['gmail.com', 'googlemail.com', 'outlook.com', 'outlook.be', 'hotmail.com', 'hotmail.be', 'hotmail.fr',
  'live.com', 'live.be', 'yahoo.com', 'yahoo.fr', 'icloud.com', 'me.com', 'proton.me', 'protonmail.com', 'gmx.com', 'gmx.net',
  'skynet.be', 'telenet.be', 'proximus.be'];

@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe],
  template: `
    <div class="m-auth">
      <div class="m-auth__main">
        <a class="m-logo" routerLink="/">
          <span class="m-logo__mark" aria-hidden="true"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round"><circle cx="6" cy="6" r="2.2"/><circle cx="18" cy="6" r="2.2"/><circle cx="12" cy="18" r="2.2"/><path d="M7.6 7.6 11 16M16.4 7.6 13 16M8.2 6h7.6"/></svg></span>
          <span class="m-logo__name">Metamind</span>
        </a>

        <div class="m-auth__form">
          @switch (mode) {
            @case ('connexion') {
              <form (ngSubmit)="login()" class="m-stack">
                <h1 class="m-title">{{ 'Connexion' | t }}</h1>
                <p class="m-muted">{{ 'Espace réservé aux bibliothécaires et administrateurs.' | t }}</p>
                <label class="m-field">{{ 'Adresse email professionnelle' | t }}
                  <input class="m-input" type="email" name="email" autocomplete="email" required [(ngModel)]="email" />
                </label>
                <label class="m-field">
                  <span class="m-row-between"><span>{{ 'Mot de passe' | t }}</span><a routerLink="/mot-de-passe" class="m-small">{{ 'Mot de passe oublié ?' | t }}</a></span>
                  <input class="m-input" type="password" name="password" autocomplete="current-password" required [(ngModel)]="password" />
                </label>
                @if (error()) { <p class="m-alert" role="alert">{{ error() }}</p> }
                <button class="m-btn m-btn--primary m-btn--block" type="submit" [disabled]="busy()">{{ 'Se connecter' | t }}</button>
                <p class="m-small m-muted m-center">{{ 'Pas encore de compte ?' | t }} <a routerLink="/inscription">{{ 'Demander un accès' | t }}</a></p>
              </form>
            }
            @case ('inscription') {
              @if (done()) {
                <div class="m-stack">
                  <h1 class="m-title">{{ 'Demande envoyée' | t }}</h1>
                  <p>{{ (institutionRequested() ? 'L\\'administrateur doit d\\'abord valider votre institution, puis votre compte. Vous recevrez un email à cette adresse dès que vous pourrez vous connecter.' : 'Un administrateur doit valider votre compte. Vous recevrez un email à cette adresse dès que vous pourrez vous connecter.') | t }}</p>
                  <a class="m-btn m-btn--ghost" routerLink="/connexion">{{ 'Retour à la connexion' | t }}</a>
                </div>
              } @else {
                <form (ngSubmit)="register()" class="m-stack">
                  <h1 class="m-title">{{ 'Demander un accès' | t }}</h1>
                  <p class="m-muted">{{ 'Utilisez l\\'adresse email de votre institution : elle sert à vous y rattacher.' | t }}</p>
                  <div class="m-grid2">
                    <label class="m-field">{{ 'Prénom' | t }}<input class="m-input" name="firstName" required [(ngModel)]="firstName" autocomplete="given-name" /></label>
                    <label class="m-field">{{ 'Nom' | t }}<input class="m-input" name="lastName" required [(ngModel)]="lastName" autocomplete="family-name" /></label>
                  </div>
                  <label class="m-field">{{ 'Adresse email institutionnelle' | t }}
                    <input class="m-input" type="email" name="email" required [(ngModel)]="email" (ngModelChange)="unknownDomain.set(false)" autocomplete="email" />
                  </label>
                  @if (personalDomain()) {
                    <p class="m-alert" role="alert">{{ 'Utilisez l\\'adresse email de votre institution, pas une adresse personnelle.' | t }}</p>
                  }
                  @if (unknownDomain()) {
                    <div class="m-sheet m-sheet--pad m-stack" role="alert">
                      <p class="m-small"><strong>{{ 'Cette adresse ne correspond à aucune institution inscrite sur Metamind' | t }} (<code>{{ domain() }}</code>).</strong></p>
                      <p class="m-small">{{ 'Vérifiez votre adresse email. Si votre institution n\\'est pas encore inscrite, indiquez son nom officiel : l\\'administrateur validera l\\'institution, puis votre compte.' | t }}</p>
                      <label class="m-field">{{ 'Nom officiel de votre institution' | t }}
                        <input class="m-input" name="institutionName" required [(ngModel)]="institutionName" autocomplete="organization" />
                      </label>
                    </div>
                  }
                  <label class="m-field">{{ 'Mot de passe' | t }} <span class="m-muted m-small">({{ '8 caractères minimum' | t }})</span>
                    <input class="m-input" type="password" name="password" minlength="8" required [(ngModel)]="password" autocomplete="new-password" />
                  </label>
                  <label class="m-check"><input type="checkbox" name="cgu" [(ngModel)]="acceptTerms" /> <span>{{ 'J\\'accepte les' | t }} <a routerLink="/legal" fragment="mentions-legales">{{ 'conditions d\\'utilisation' | t }}</a></span></label>
                  <label class="m-check"><input type="checkbox" name="privacy" [(ngModel)]="acceptPrivacy" /> <span>{{ 'J\\'ai lu la' | t }} <a routerLink="/legal" fragment="confidentialite">{{ 'politique de confidentialité' | t }}</a></span></label>
                  @if (error()) { <p class="m-alert" role="alert">{{ error() }}</p> }
                  <button class="m-btn m-btn--primary m-btn--block" type="submit" [disabled]="busy() || !canRegister()">{{ (unknownDomain() ? 'Demander l\\'ajout de mon institution' : 'Envoyer la demande') | t }}</button>
                  <p class="m-small m-muted m-center">{{ 'Déjà un compte ?' | t }} <a routerLink="/connexion">{{ 'Se connecter' | t }}</a></p>
                </form>
              }
            }
            @case ('oubli') {
              <form (ngSubmit)="requestReset()" class="m-stack">
                <h1 class="m-title">{{ 'Mot de passe oublié' | t }}</h1>
                @if (done()) {
                  <p>{{ 'Si un compte actif existe pour cette adresse, un lien valable 30 minutes vient d\\'être envoyé. Pensez à regarder dans les courriers indésirables.' | t }}</p>
                } @else {
                  <label class="m-field">{{ 'Adresse email professionnelle' | t }}<input class="m-input" type="email" name="email" required [(ngModel)]="email" /></label>
                  <button class="m-btn m-btn--primary m-btn--block" type="submit" [disabled]="busy()">{{ 'Envoyer le lien' | t }}</button>
                }
                <a routerLink="/connexion" class="m-small">{{ 'Retour à la connexion' | t }}</a>
              </form>
            }
            @case ('reinitialiser') {
              <form (ngSubmit)="confirmReset()" class="m-stack">
                <h1 class="m-title">{{ 'Nouveau mot de passe' | t }}</h1>
                @if (done()) {
                  <p>{{ 'Mot de passe modifié. Vous pouvez vous connecter.' | t }}</p>
                  <a class="m-btn m-btn--primary" routerLink="/connexion">{{ 'Se connecter' | t }}</a>
                } @else {
                  <label class="m-field">{{ 'Mot de passe' | t }} <span class="m-muted m-small">({{ '8 caractères minimum' | t }})</span>
                    <input class="m-input" type="password" name="password" minlength="8" required [(ngModel)]="password" autocomplete="new-password" />
                  </label>
                  @if (error()) { <p class="m-alert" role="alert">{{ error() }}</p> }
                  <button class="m-btn m-btn--primary m-btn--block" type="submit" [disabled]="busy() || password.length < 8">{{ 'Enregistrer' | t }}</button>
                }
              </form>
            }
          }
        </div>

        <div class="m-langs m-langs--plain">
          @for (lang of languages; track lang) {
            <button type="button" [class.is-active]="i18n.lang() === lang" (click)="i18n.setLanguage(lang)">{{ lang.toUpperCase() }}</button>
          }
          <a routerLink="/legal" fragment="confidentialite" class="m-small">{{ 'Confidentialité' | t }}</a>
        </div>
      </div>

      <aside class="m-auth__side">
        <ol class="m-steps">
          <li><span>1</span><div><strong>{{ 'Importer' | t }}</strong><p>{{ 'Déposez vos PDF, DOCX ou TXT, un ou plusieurs à la fois.' | t }}</p></div></li>
          <li><span>2</span><div><strong>{{ 'Relire' | t }}</strong><p>{{ 'L\\'IA propose la notice et indique ce qui est à relire.' | t }}</p></div></li>
          <li><span>3</span><div><strong>{{ 'Publier' | t }}</strong><p>{{ 'La fiche rejoint le catalogue, public ou réservé à votre institution.' | t }}</p></div></li>
        </ol>
      </aside>
    </div>
  `
})
export class AuthPage {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly i18n = inject(I18nService);
  readonly languages: Language[] = ['fr', 'nl', 'en'];
  readonly mode = this.route.snapshot.data['mode'] as Mode;

  email = '';
  password = '';
  firstName = '';
  lastName = '';
  acceptTerms = false;
  acceptPrivacy = false;
  institutionName = '';
  /** Le domaine de l'adresse ne correspond à aucune institution : on demande son nom. */
  readonly unknownDomain = signal(false);
  readonly institutionRequested = signal(false);
  readonly busy = signal(false);
  readonly done = signal(false);
  readonly error = signal('');

  login(): void {
    this.busy.set(true);
    this.error.set('');
    this.session.login({ email: this.email.trim(), password: this.password }).subscribe({
      next: () => this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('retour') ?? this.session.homeUrl()),
      error: (e) => {
        this.busy.set(false);
        this.error.set(this.i18n.t(e?.status === 429 ? 'Trop de tentatives. Réessayez dans quelques minutes.'
          : e?.status === 403 ? (e?.error?.message?.includes('attend') ? 'Votre compte attend la validation d\'un administrateur.' : 'Ce compte a été désactivé. Contactez l\'administrateur de la plateforme.')
          : 'Email ou mot de passe incorrect.'));
      }
    });
  }

  /** Une adresse personnelle ne rattache à aucune institution : refusée avant l'envoi, comme le fait le serveur. */
  personalDomain(): boolean {
    return PERSONAL_EMAIL_DOMAINS.includes(this.domain());
  }

  domain(): string {
    return this.email.trim().split('@')[1]?.toLowerCase() ?? '';
  }

  canRegister(): boolean {
    return !!this.firstName.trim() && !!this.lastName.trim() && /.+@.+\..+/.test(this.email) && !this.personalDomain() && this.password.length >= 8 && this.acceptTerms && this.acceptPrivacy
      && (!this.unknownDomain() || !!this.institutionName.trim());
  }

  /** B1 : l'institution est déduite du domaine de l'email ; un domaine inconnu devient une demande d'institution. */
  register(): void {
    if (!this.canRegister()) return;
    this.busy.set(true);
    this.error.set('');
    const name = this.unknownDomain() ? this.institutionName.trim() : undefined;
    this.api.register({ firstName: this.firstName.trim(), lastName: this.lastName.trim(), email: this.email.trim(), institution: this.domain(), password: this.password, langue: this.i18n.lang(),
      ...(name ? { nom_institution: name } : {}) }).subscribe({
      next: () => { this.busy.set(false); this.institutionRequested.set(!!name); this.done.set(true); },
      error: (e) => {
        this.busy.set(false);
        if (e?.status === 404) { this.unknownDomain.set(true); return; }
        this.error.set(this.i18n.t(this.registrationError(e?.status, e?.error?.message ?? '')));
      }
    });
  }

  private registrationError(status: number, message: string): string {
    if (status === 409) return message.includes('institution') ? 'Une institution porte déjà ce nom avec un autre domaine email. Contactez l\'administrateur.' : 'Un compte existe déjà avec cette adresse.';
    if (message.includes('personnelle')) return 'Utilisez l\'adresse email de votre institution, pas une adresse personnelle.';
    if (message.includes('inactive')) return 'Cette institution a été désactivée. Contactez l\'administrateur de la plateforme.';
    if (message.includes('domaine')) return 'Cette adresse ne correspond pas au domaine de l\'institution.';
    return 'La demande n\'a pas pu être envoyée.';
  }

  requestReset(): void {
    this.busy.set(true);
    this.api.requestPasswordReset(this.email.trim()).subscribe({
      next: () => { this.busy.set(false); this.done.set(true); },
      error: () => { this.busy.set(false); this.done.set(true); }
    });
  }

  confirmReset(): void {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.busy.set(true);
    this.api.confirmPasswordReset(token, this.password).subscribe({
      next: () => { this.busy.set(false); this.done.set(true); },
      error: () => { this.busy.set(false); this.error.set(this.i18n.t('Ce lien a expiré ou a déjà été utilisé.')); }
    });
  }
}

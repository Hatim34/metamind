import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, AuditLog, Institution, InstitutionPhoto, UserSession } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { SessionService } from '../core/session.service';
import { ToastService } from '../core/toast.service';
import { ConfirmService } from '../core/confirm.service';

type Tab = 'comptes' | 'utilisateurs' | 'institutions' | 'configuration' | 'journal';

/** Administration (A1 à A4). */
@Component({
  standalone: true,
  imports: [FormsModule, DatePipe, NgTemplateOutlet, TranslatePipe],
  template: `
    <div class="m-wrap m-page m-split m-split--admin">
      <nav class="m-sidenav" [attr.aria-label]="'Administration' | t">
        @for (item of menu(); track item.id) {
          <button type="button" [class.is-on]="tab() === item.id" [attr.aria-current]="tab() === item.id ? 'page' : null" (click)="tab.set(item.id)">
            <span>{{ item.label | t }}</span>@if (item.count !== null) { <span class="m-muted">{{ item.count }}</span> }
          </button>
        }
      </nav>

      <section class="m-admin">
        @switch (tab()) {
          @case ('comptes') {
            <h1 class="m-title">{{ 'Comptes à valider' | t }}</h1>
            <p class="m-muted">{{ 'L\\'institution est déduite du domaine de l\\'adresse email. Validez ou refusez chaque demande.' | t }}</p>
            <ng-container *ngTemplateOutlet="userTable; context: { $implicit: pending() }"></ng-container>
          }
          @case ('utilisateurs') {
            <h1 class="m-title">{{ 'Utilisateurs' | t }}</h1>
            <ng-container *ngTemplateOutlet="userTable; context: { $implicit: users() }"></ng-container>
          }
          @case ('institutions') {
            <h1 class="m-title">{{ 'Institutions' | t }}</h1>
            <form class="m-sheet m-sheet--pad m-inline-form" (ngSubmit)="createInstitution()">
              <label class="m-field">{{ 'Code' | t }}<input class="m-input" name="code" [(ngModel)]="inst.code" required /></label>
              <label class="m-field">{{ 'Nom' | t }}<input class="m-input" name="name" [(ngModel)]="inst.name" required /></label>
              <label class="m-field">{{ 'Domaine email' | t }}<input class="m-input" name="domain" [(ngModel)]="inst.emailDomain" placeholder="ulb.be" required /></label>
              <button type="submit" class="m-btn m-btn--primary" [disabled]="!inst.code || !inst.name || !inst.emailDomain">{{ 'Ajouter' | t }}</button>
            </form>
            <div class="m-grid-cards">
              @for (i of institutions(); track i.id) {
                <div class="m-sheet m-sheet--pad m-stack">
                  @if (photoOf(i.id); as photo) { <img class="m-inst-photo" [src]="photo.photoUrl" alt="" /> }
                  <div class="m-row-between"><strong>{{ i.name }}</strong><span [class]="i.active ? 'm-ok' : 'm-ko'">{{ (i.active ? 'Active' : 'Désactivée') | t }}</span></div>
                  <code class="m-small">{{ i.emailDomain }}</code>
                  @if (i.active) {
                    <label class="m-field m-small">{{ 'Photo (JPEG, PNG ou WebP, 5 Mo au plus)' | t }}
                      <input type="file" accept="image/jpeg,image/png,image/webp" (change)="pickPhoto(i.id, $event)" />
                    </label>
                    <label class="m-field m-small">{{ 'Crédit de la photo (auteur, licence, source)' | t }}
                      <input class="m-input m-input--sm" [name]="'credit' + i.id" [(ngModel)]="credits[i.id]" />
                    </label>
                    <button type="button" class="m-btn m-btn--sm" [disabled]="!pickedPhotos[i.id] || !credits[i.id]?.trim()" (click)="savePhoto(i.id)">{{ 'Enregistrer la photo' | t }}</button>
                  }
                  @if (i.active) { <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="deactivateInstitution(i)">{{ 'Désactiver' | t }}</button> }
                </div>
              }
            </div>
          }
          @case ('configuration') {
            <h1 class="m-title">{{ 'Configuration' | t }}</h1>
            <p class="m-muted">{{ 'Chaque modification s\\'applique immédiatement. Les clés API, Stripe et DSpace restent dans les variables d\\'environnement du serveur.' | t }}</p>
            <form class="m-sheet m-sheet--pad m-stack" (ngSubmit)="saveConfig()">
              @for (key of configKeys(); track key) {
                <label class="m-field">{{ (configLabels[key] ?? key) | t }}<input class="m-input" [name]="key" [(ngModel)]="config[key]" [type]="key === 'modele_llm' ? 'text' : 'number'" /></label>
              } @empty { <p class="m-muted">{{ 'Aucun paramètre.' | t }}</p> }
              <div class="m-actions"><button type="submit" class="m-btn m-btn--primary">{{ 'Enregistrer' | t }}</button>
                <button type="button" class="m-btn m-btn--ghost" (click)="exportCsv()">{{ 'Exporter les documents (CSV)' | t }}</button></div>
            </form>
          }
          @case ('journal') {
            <h1 class="m-title">{{ 'Journal d\\'audit' | t }}</h1>
            <input class="m-input m-input--search" [(ngModel)]="logFilter" name="logFilter" [placeholder]="'Filtrer par action ou entité' | t" />
            <section class="m-sheet m-scroll">
              <table class="m-table">
                <thead><tr><th>{{ 'Date' | t }}</th><th>{{ 'Action' | t }}</th><th>{{ 'Entité' | t }}</th><th>{{ 'Détails' | t }}</th></tr></thead>
                <tbody>
                  @for (log of filteredLogs(); track log.id) {
                    <tr><td class="m-muted">{{ log.date_creation | date: 'dd/MM/yyyy HH:mm' }}</td><td><code>{{ log.action }}</code></td><td>{{ log.type_entite }} @if (log.entite_id) { #{{ log.entite_id }} }</td><td class="m-small">{{ log.details }}</td></tr>
                  }
                </tbody>
              </table>
            </section>
          }
        }
      </section>
    </div>

    <ng-template #userTable let-list>
      <section class="m-sheet m-scroll">
        <table class="m-table">
          <thead><tr><th>{{ 'Nom' | t }}</th><th>{{ 'Email' | t }}</th><th>{{ 'Institution' | t }}</th><th>{{ 'Rôle' | t }}</th><th><span class="m-sr">{{ 'Actions' | t }}</span></th></tr></thead>
          <tbody>
            @for (u of list; track u.id) {
              <tr>
                <td><strong>{{ u.firstName }} {{ u.lastName }}</strong></td>
                <td><code class="m-small">{{ u.email }}</code></td>
                <td>{{ u.institution }}</td>
                <td>
                  @if (u.id === session.user()?.id) { {{ 'Administrateur' | t }} } @else {
                  <select class="m-input m-input--sm" [ngModel]="u.role" (ngModelChange)="changeRole(u, $event)" [name]="'role' + u.id" [attr.aria-label]="'Rôle' | t">
                    <option value="LIBRARIAN">{{ 'Bibliothécaire' | t }}</option>
                    <option value="ADMIN">{{ 'Administrateur' | t }}</option>
                  </select>
                  }
                </td>
                <td class="m-right">
                  @if (u.id === session.user()?.id) { <span class="m-muted m-small">{{ 'Votre compte' | t }}</span> } @else {
                  <span class="m-actions m-actions--row">
                    @if (u.status !== 'ACTIF') { <button type="button" class="m-btn m-btn--primary m-btn--sm" (click)="setStatus(u, 'ACTIF')">{{ 'Activer' | t }}</button> }
                    @if (u.status !== 'DESACTIVE') { <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="setStatus(u, 'DESACTIVE')">{{ (u.status === 'EN_ATTENTE' ? 'Refuser' : 'Désactiver') | t }}</button> }
                  </span>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="m-empty">{{ 'Aucun compte.' | t }}</td></tr>
            }
          </tbody>
        </table>
      </section>
    </ng-template>
  `
})
export class AdminPage {
  private readonly api = inject(ApiService);
  readonly session = inject(SessionService);
  /** Paramètres réellement appliqués par le serveur (cahier des charges A2). */
  readonly configLabels: Record<string, string | undefined> = {
    modele_llm: 'Modèle Gemini utilisé pour l\'extraction',
    taille_max_upload_mo: 'Taille maximale d\'un fichier importé (Mo, 50 au plus)',
    tentatives_connexion_max: 'Échecs de connexion avant blocage de 15 minutes',
    jwt_duree_secondes: 'Durée d\'une session (secondes)'
  };
  private readonly toasts = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly i18n = inject(I18nService);

  readonly tab = signal<Tab>('comptes');
  readonly users = signal<UserSession[]>([]);
  readonly institutions = signal<Institution[]>([]);
  readonly photos = signal<InstitutionPhoto[]>([]);
  credits: Record<number, string | undefined> = {};
  pickedPhotos: Record<number, File> = {};
  readonly logs = signal<AuditLog[]>([]);
  readonly configKeys = signal<string[]>([]);
  readonly pending = computed(() => this.users().filter((u) => u.status === 'EN_ATTENTE'));
  readonly menu = computed(() => [
    { id: 'comptes' as Tab, label: 'Comptes à valider', count: this.pending().length },
    { id: 'utilisateurs' as Tab, label: 'Utilisateurs', count: this.users().length },
    { id: 'institutions' as Tab, label: 'Institutions', count: this.institutions().length },
    { id: 'configuration' as Tab, label: 'Configuration', count: null },
    { id: 'journal' as Tab, label: 'Journal d\'audit', count: null }
  ]);
  config: Record<string, string> = {};
  inst = { code: '', name: '', emailDomain: '' };
  logFilter = '';

  constructor() {
    this.loadUsers();
    this.loadInstitutions();
    this.api.getAdminLogs().subscribe({ next: (l) => this.logs.set(l), error: () => undefined });
    this.api.getAdminConfig().subscribe({ next: (c) => { this.config = { ...c }; this.configKeys.set(Object.keys(c)); }, error: () => undefined });
  }

  filteredLogs(): AuditLog[] {
    const f = this.logFilter.trim().toLowerCase();
    return f ? this.logs().filter((l) => (l.action + ' ' + l.type_entite + ' ' + l.details).toLowerCase().includes(f)) : this.logs();
  }

  loadUsers(): void { this.api.getAdminUsers().subscribe({ next: (u) => this.users.set(u), error: () => undefined }); }
  loadInstitutions(): void {
    this.api.getInstitutions().subscribe({ next: (i) => this.institutions.set(i), error: () => undefined });
    this.api.getInstitutionPhotos().subscribe({
      next: (photos) => {
        this.photos.set(photos);
        photos.forEach((p) => { this.credits[p.institutionId] ??= p.credit; });
      },
      error: () => undefined
    });
  }

  photoOf(institutionId: number): InstitutionPhoto | undefined {
    return this.photos().find((p) => p.institutionId === institutionId);
  }

  pickPhoto(institutionId: number, event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.pickedPhotos[institutionId] = file;
  }

  savePhoto(institutionId: number): void {
    const file = this.pickedPhotos[institutionId];
    if (!file) return;
    this.api.uploadInstitutionPhoto(institutionId, file, (this.credits[institutionId] ?? '').trim()).subscribe({
      next: () => { delete this.pickedPhotos[institutionId]; this.toasts.show(this.i18n.t('Photo enregistrée.')); this.loadInstitutions(); },
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  async setStatus(user: UserSession, statut: 'ACTIF' | 'DESACTIVE'): Promise<void> {
    if (statut === 'DESACTIVE') {
      const refusing = user.status === 'EN_ATTENTE';
      const accepted = await this.confirm.ask({
        title: this.i18n.t(refusing ? 'Refuser cette demande de compte ?' : 'Désactiver ce compte ?'),
        message: `${user.firstName} ${user.lastName} (${user.email}) ${this.i18n.t(refusing ? 'n\'aura pas accès à l\'espace bibliothécaire. Vous pourrez encore activer ce compte plus tard.' : 'ne pourra plus se connecter. Le compte pourra être réactivé plus tard.')}`,
        action: this.i18n.t(refusing ? 'Refuser' : 'Désactiver')
      });
      if (!accepted) return;
    }
    this.api.updateAdminUser(user.id, { statut }).subscribe({
      next: () => { this.toasts.show(this.i18n.t(statut === 'ACTIF' ? 'Compte activé.' : 'Compte désactivé.')); this.loadUsers(); },
      error: () => this.toasts.show(this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  changeRole(user: UserSession, role: 'LIBRARIAN' | 'ADMIN'): void {
    this.api.updateAdminUser(user.id, { role }).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Rôle modifié.')); this.loadUsers(); },
      error: () => this.toasts.show(this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  createInstitution(): void {
    this.api.createInstitution({ ...this.inst, emailDomain: this.inst.emailDomain.trim().toLowerCase() }).subscribe({
      next: () => { this.inst = { code: '', name: '', emailDomain: '' }; this.loadInstitutions(); },
      error: () => this.toasts.show(this.i18n.t('Création impossible : ce domaine existe peut-être déjà.'), 'error')
    });
  }

  async deactivateInstitution(i: Institution): Promise<void> {
    const accepted = await this.confirm.ask({
      title: this.i18n.t('Désactiver cette institution ?'),
      message: `${i.name} ${this.i18n.t('n\'acceptera plus de nouvelles inscriptions. Ses publications restent dans le catalogue.')}`,
      action: this.i18n.t('Désactiver')
    });
    if (!accepted) return;
    this.api.deactivateInstitution(i.id).subscribe({ next: () => this.loadInstitutions(), error: () => undefined });
  }

  saveConfig(): void {
    const values = Object.fromEntries(Object.entries(this.config).map(([key, value]) => [key, String(value)]));
    this.api.updateAdminConfig(values).subscribe({
      next: (saved) => { this.config = { ...saved }; this.toasts.show(this.i18n.t('Configuration enregistrée.')); },
      // Le serveur explique pourquoi une valeur est refusée (plage autorisée, modèle invalide).
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  exportCsv(): void {
    this.api.exportAdminDocumentsCsv().subscribe((csv) => {
      const link = document.createElement('a');
      link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
      link.download = 'documents.csv';
      link.click();
    });
  }
}

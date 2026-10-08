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
            <h1 class="m-title">{{ 'Demandes' | t }}</h1>
            @if (requestedInstitutions().length) {
              <h2 class="m-h3">{{ 'Institutions à ajouter' | t }}</h2>
              <p class="m-muted">{{ 'Demandées à l\\'inscription par une personne dont l\\'institution n\\'était pas encore inscrite. Valider l\\'institution active aussi le compte qui l\\'a demandée et lui accorde 20 crédits.' | t }}</p>
              <div class="m-grid-cards">
                @for (i of requestedInstitutions(); track i.id) {
                  <div class="m-sheet m-sheet--pad m-stack">
                    <div class="m-row-between"><strong>{{ i.name }}</strong><span class="m-warn">{{ 'En attente' | t }}</span></div>
                    <code class="m-small">{{ i.emailDomain }}</code>
                    @for (u of requestersOf(i); track u.id) { <span class="m-small">{{ 'Demandée par' | t }} {{ u.firstName }} {{ u.lastName }} · <code>{{ u.email }}</code></span> }
                    <div class="m-actions m-actions--row">
                      <button type="button" class="m-btn m-btn--primary m-btn--sm" (click)="approveInstitution(i)">{{ 'Valider' | t }}</button>
                      <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="refuseInstitution(i)">{{ 'Refuser' | t }}</button>
                    </div>
                  </div>
                }
              </div>
              <h2 class="m-h3">{{ 'Comptes à valider' | t }}</h2>
            }
            <p class="m-muted">{{ 'L\\'institution est déduite du domaine de l\\'adresse email. Validez ou refusez chaque demande.' | t }}</p>
            <ng-container *ngTemplateOutlet="userTable; context: { $implicit: pending() }"></ng-container>
          }
          @case ('utilisateurs') {
            <h1 class="m-title">{{ 'Utilisateurs' | t }}</h1>
            <div class="m-actions m-actions--row">
              <input class="m-input m-input--search" name="userFilter" [(ngModel)]="userFilter" [placeholder]="'Rechercher un nom ou une adresse' | t" />
              <select class="m-input m-input--sm" name="userInstitution" [(ngModel)]="userInstitution" [attr.aria-label]="'Institution' | t">
                <option value="">{{ 'Toutes les institutions' | t }}</option>
                @for (i of institutions(); track i.id) { <option [value]="i.name">{{ i.name }}</option> }
              </select>
            </div>
            <ng-container *ngTemplateOutlet="userTable; context: { $implicit: filteredUsers() }"></ng-container>
          }
          @case ('institutions') {
            <h1 class="m-title">{{ 'Institutions' | t }}</h1>
            <p class="m-muted">{{ 'Une institution créée ici est active tout de suite et reçoit 20 crédits. Ses bibliothécaires s\\'inscrivent ensuite avec une adresse de ce domaine.' | t }}</p>
            <form class="m-sheet m-sheet--pad m-inline-form" (ngSubmit)="createInstitution()">
              <label class="m-field">{{ 'Nom' | t }}<input class="m-input" name="name" [(ngModel)]="inst.name" required /></label>
              <label class="m-field">{{ 'Domaine email' | t }}<input class="m-input" name="domain" [(ngModel)]="inst.emailDomain" required /></label>
              <button type="submit" class="m-btn m-btn--primary" [disabled]="!inst.name.trim() || !inst.emailDomain.trim()">{{ 'Ajouter' | t }}</button>
            </form>
            <div class="m-grid-cards">
              @for (i of listedInstitutions(); track i.id) {
                <div class="m-sheet m-sheet--pad m-stack">
                  @if (photoOf(i.id); as photo) { <img class="m-inst-photo" [src]="photo.photoUrl" alt="" /> }
                  <div class="m-row-between"><strong>{{ i.name }}</strong><span [class]="i.active ? 'm-ok' : 'm-ko'">{{ (i.active ? 'Active' : 'Désactivée') | t }}</span></div>
                  <span class="m-small"><code>{{ i.emailDomain }}</code> · {{ usersOf(i) }} {{ 'comptes' | t }} · {{ i.creditBalance }} {{ 'crédits' | t }}</span>
                  @if (i.active) {
                    <details class="m-details">
                      <summary>{{ 'Ajuster les crédits' | t }}</summary>
                      <div class="m-stack">
                        <label class="m-field m-small">{{ 'Nombre de crédits (négatif pour en retirer)' | t }}
                          <input class="m-input m-input--sm" type="number" [name]="'amount' + i.id" [(ngModel)]="adjustAmounts[i.id]" />
                        </label>
                        <label class="m-field m-small">{{ 'Motif (visible dans l\\'historique de l\\'institution)' | t }}
                          <input class="m-input m-input--sm" [name]="'reason' + i.id" [(ngModel)]="adjustReasons[i.id]" maxlength="255" />
                        </label>
                        <button type="button" class="m-btn m-btn--sm" [disabled]="!adjustAmounts[i.id] || !adjustReasons[i.id]?.trim()" (click)="adjustCredits(i)">{{ 'Appliquer' | t }}</button>
                      </div>
                    </details>
                    <details class="m-details">
                      <summary>{{ (photoOf(i.id) ? 'Changer la photo' : 'Ajouter une photo') | t }}</summary>
                      <div class="m-stack">
                        <div class="m-field m-small">
                          <span>{{ 'Photo (JPEG, PNG ou WebP, 5 Mo au plus)' | t }}</span>
                          <span class="m-file-pick">
                            <label class="m-btn m-btn--ghost m-btn--sm">{{ 'Choisir une image' | t }}
                              <input class="m-sr" type="file" accept="image/jpeg,image/png,image/webp" (change)="pickPhoto(i.id, $event)" />
                            </label>
                            <span class="m-file-pick__name">{{ pickedPhotos[i.id]?.name ?? ('Aucun fichier choisi' | t) }}</span>
                          </span>
                        </div>
                        <label class="m-field m-small">{{ 'Crédit de la photo (auteur, licence, source)' | t }}
                          <input class="m-input m-input--sm" [name]="'credit' + i.id" [(ngModel)]="credits[i.id]" />
                        </label>
                        <button type="button" class="m-btn m-btn--sm" [disabled]="!pickedPhotos[i.id] || !credits[i.id]?.trim()" (click)="savePhoto(i.id)">{{ 'Enregistrer la photo' | t }}</button>
                      </div>
                    </details>
                    @if (i.id !== ownInstitutionId()) { <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="deactivateInstitution(i)">{{ 'Désactiver' | t }}</button> }
                  } @else {
                    <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="reactivateInstitution(i)">{{ 'Réactiver' | t }}</button>
                  }
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
            <input class="m-input m-input--search" [(ngModel)]="logFilter" (ngModelChange)="searchLogs()" name="logFilter" [placeholder]="'Filtrer par action, personne, élément ou détail' | t" />
            <p class="m-muted m-small">{{ logs().length }} / {{ logTotal() }} {{ 'entrées' | t }}</p>
            <section class="m-sheet m-scroll">
              <table class="m-table m-table--log">
                <thead><tr><th>{{ 'Date' | t }}</th><th>{{ 'Par' | t }}</th><th>{{ 'Action' | t }}</th><th>{{ 'Élément' | t }}</th><th>{{ 'Détails' | t }}</th></tr></thead>
                <tbody>
                  @for (log of logs(); track log.id) {
                    <tr>
                      <td class="m-muted m-nowrap">{{ log.date_creation | date: 'dd/MM/yyyy HH:mm' }}</td>
                      <td class="m-small">{{ log.auteur ?? ('Système' | t) }}</td>
                      <td>{{ (actionLabels[log.action] ?? log.action) | t }}</td>
                      <td class="m-small">
                        {{ (entityLabels[log.type_entite] ?? log.type_entite) | t }} @if (log.entite_id) { #{{ log.entite_id }} }
                        @if (log.libelle_entite) { <br /><span class="m-muted">{{ log.libelle_entite }}</span> }
                      </td>
                      <td class="m-small">
                        @if (log.action === 'MODIFICATION_METADONNEE') {
                          @let change = metadataChange(log.details);
                          <strong>{{ (fieldLabels[change.field] ?? change.field) | t }}</strong>
                          @if (change.field === 'rejet') { : {{ change.after }} }
                          @else {
                            <div class="m-log-change"><span class="m-muted">{{ 'Avant' | t }} :</span> {{ change.before ? shorten(change.before) : '-' }}</div>
                            <div class="m-log-change"><span class="m-muted">{{ 'Après' | t }} :</span> {{ change.after ? shorten(change.after) : '-' }}</div>
                            @if (change.before.length > 140 || change.after.length > 140) {
                              <details class="m-log-full"><summary>{{ 'Voir le texte complet' | t }}</summary>
                                <p><span class="m-muted">{{ 'Avant' | t }} :</span> {{ change.before }}</p>
                                <p><span class="m-muted">{{ 'Après' | t }} :</span> {{ change.after }}</p>
                              </details>
                            }
                          }
                        } @else { {{ log.details }} }
                      </td>
                    </tr>
                  } @empty { <tr><td colspan="5" class="m-muted">{{ 'Aucune entrée.' | t }}</td></tr> }
                </tbody>
              </table>
            </section>
            @if (logs().length < logTotal()) {
              <div class="m-actions"><button type="button" class="m-btn m-btn--ghost" (click)="loadLogs(logPage() + 1)">{{ 'Afficher les entrées plus anciennes' | t }}</button></div>
            }
          }
        }
      </section>
    </div>

    <ng-template #userTable let-list>
      <section class="m-sheet m-scroll">
        <table class="m-table">
          <thead><tr><th>{{ 'Nom' | t }}</th><th>{{ 'Email' | t }}</th><th>{{ 'Institution' | t }}</th><th>{{ 'Rôle' | t }}</th><th>{{ 'Statut' | t }}</th><th><span class="m-sr">{{ 'Actions' | t }}</span></th></tr></thead>
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
                <td><span [class]="u.status === 'ACTIF' ? 'm-ok' : u.status === 'EN_ATTENTE' ? 'm-warn' : 'm-ko'">{{ statusLabel(u.status) | t }}</span></td>
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
              <tr><td colspan="6" class="m-empty">{{ 'Aucun compte.' | t }}</td></tr>
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
  readonly logTotal = signal(0);
  readonly logPage = signal(0);
  private logSearchTimer: ReturnType<typeof setTimeout> | undefined;
  readonly configKeys = signal<string[]>([]);
  readonly requestedInstitutions = computed(() => this.institutions().filter((i) => i.pending));
  /** Comptes à valider un par un : ceux d'une institution demandée sont tranchés avec elle. */
  readonly pending = computed(() => {
    const requested = new Set(this.requestedInstitutions().map((i) => i.name));
    return this.users().filter((u) => u.status === 'EN_ATTENTE' && !requested.has(u.institution));
  });
  readonly listedInstitutions = computed(() => this.institutions().filter((i) => !i.pending));
  readonly ownInstitutionId = computed(() => this.institutions().find((i) => i.name === this.session.user()?.institution)?.id);
  readonly menu = computed(() => [
    { id: 'comptes' as Tab, label: 'Demandes', count: this.pending().length + this.requestedInstitutions().length },
    { id: 'utilisateurs' as Tab, label: 'Utilisateurs', count: this.users().length },
    { id: 'institutions' as Tab, label: 'Institutions', count: this.listedInstitutions().length },
    { id: 'configuration' as Tab, label: 'Configuration', count: null },
    { id: 'journal' as Tab, label: 'Journal d\'audit', count: null }
  ]);
  config: Record<string, string> = {};
  inst = { name: '', emailDomain: '' };
  logFilter = '';
  userFilter = '';
  userInstitution = '';
  adjustAmounts: Record<number, number | undefined> = {};
  adjustReasons: Record<number, string | undefined> = {};
  readonly statusLabels: Record<UserSession['status'], string> = { ACTIF: 'Actif', EN_ATTENTE: 'En attente', DESACTIVE: 'Désactivé' };
  readonly actionLabels: Record<string, string> = {
    AJUSTEMENT_CREDITS: 'Ajustement de crédits',
    MODIFICATION_CONFIGURATION: 'Configuration modifiée',
    MODIFICATION_INSTITUTION: 'Institution modifiée',
    MODIFICATION_METADONNEE: 'Notice modifiée',
    MODIFICATION_UTILISATEUR: 'Compte modifié',
    PUBLICATION_NOTICE: 'Notice publiée',
    SUPPRESSION_DOCUMENT: 'Document supprimé',
    SUPPRESSION_COMPTE: 'Compte supprimé',
    RETRAIT_PUBLICATION: 'Retirée du catalogue'
  };
  readonly fieldLabels: Record<string, string> = {
    titre: 'Titre', resume: 'Résumé', auteurs: 'Auteurs', mots_cles: 'Mots-clés', classification: 'Classification',
    date_publication: 'Date de publication', type_document: 'Type', langue: 'Langue', doi: 'DOI', visibilite: 'Visibilité', rejet: 'Motif du rejet'
  };
  readonly entityLabels: Record<string, string> = {
    users: 'Compte', institutions: 'Institution', configurations: 'Configuration', documents: 'Document', metadonnees: 'Notice', credits: 'Crédits'
  };

  constructor() {
    this.loadUsers();
    this.loadInstitutions();
    this.loadLogs(0);
    this.api.getAdminConfig().subscribe({ next: (c) => { this.config = { ...c }; this.configKeys.set(Object.keys(c)); }, error: () => undefined });
  }

  statusLabel(status: string): string {
    return this.statusLabels[status as UserSession['status']] ?? status;
  }

  filteredUsers(): UserSession[] {
    const f = this.userFilter.trim().toLowerCase();
    return this.users().filter((u) => (!this.userInstitution || u.institution === this.userInstitution)
      && (!f || `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(f)));
  }

  requestersOf(i: Institution): UserSession[] {
    return this.users().filter((u) => u.institution === i.name && u.status === 'EN_ATTENTE');
  }

  usersOf(i: Institution): number {
    return this.users().filter((u) => u.institution === i.name && u.status === 'ACTIF').length;
  }

  async approveInstitution(i: Institution): Promise<void> {
    const people = this.requestersOf(i).map((u) => `${u.firstName} ${u.lastName}`).join(', ');
    const accepted = await this.confirm.ask({
      title: this.i18n.t('Valider cette institution ?'),
      message: `${i.name} (${i.emailDomain}) ${this.i18n.t('devient active et reçoit 20 crédits.')}${people ? ' ' + this.i18n.t('Comptes activés :') + ' ' + people + '.' : ''}`,
      action: this.i18n.t('Valider')
    });
    if (!accepted) return;
    this.api.setInstitutionActive(i.id, true).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Institution validée.')); this.loadInstitutions(); this.loadUsers(); },
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  async refuseInstitution(i: Institution): Promise<void> {
    const accepted = await this.confirm.ask({
      title: this.i18n.t('Refuser cette institution ?'),
      message: `${i.name} (${i.emailDomain}) ${this.i18n.t('ne sera pas ajoutée et les comptes qui l\'ont demandée seront refusés.')}`,
      action: this.i18n.t('Refuser')
    });
    if (!accepted) return;
    this.api.setInstitutionActive(i.id, false).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Demande refusée.')); this.loadInstitutions(); this.loadUsers(); },
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  reactivateInstitution(i: Institution): void {
    this.api.setInstitutionActive(i.id, true).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Institution réactivée.')); this.loadInstitutions(); },
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  async adjustCredits(i: Institution): Promise<void> {
    const amount = Math.trunc(Number(this.adjustAmounts[i.id] ?? 0));
    const reason = (this.adjustReasons[i.id] ?? '').trim();
    if (!amount || !reason) return;
    const accepted = await this.confirm.ask({
      title: this.i18n.t(amount > 0 ? 'Ajouter des crédits ?' : 'Retirer des crédits ?'),
      message: `${i.name} : ${i.creditBalance} → ${i.creditBalance + amount} ${this.i18n.t('crédits')}. ${this.i18n.t('Motif')} : ${reason}`,
      action: this.i18n.t('Appliquer')
    });
    if (!accepted) return;
    this.api.adjustInstitutionCredits(i.id, amount, reason).subscribe({
      next: () => { delete this.adjustAmounts[i.id]; delete this.adjustReasons[i.id]; this.toasts.show(this.i18n.t('Crédits ajustés.')); this.loadInstitutions(); },
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  /** Le journal est paginé et filtré par le serveur : le filtre porte sur toutes les entrées, pas seulement celles affichées. */
  loadLogs(page: number): void {
    this.api.getAdminLogs(page, this.logFilter.trim()).subscribe({
      next: (r) => {
        this.logs.set(page === 0 ? r.contenu : [...this.logs(), ...r.contenu]);
        this.logTotal.set(r.total_elements);
        this.logPage.set(page);
      },
      error: () => undefined
    });
  }

  searchLogs(): void {
    clearTimeout(this.logSearchTimer);
    this.logSearchTimer = setTimeout(() => this.loadLogs(0), 300);
  }

  /** Une modification de notice est enregistrée en trois lignes : champ, ancienne valeur, nouvelle valeur. */
  metadataChange(details: string): { field: string; before: string; after: string } {
    const [field = '', before = '', after = ''] = (details ?? '').split('\n');
    return { field, before, after };
  }

  shorten(value: string): string {
    return value.length > 140 ? value.slice(0, 140) + '…' : value;
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
      error: (e) => this.toasts.show(e?.error?.status === 409 || e?.status === 409 ? this.i18n.t('Validez d\'abord la demande d\'institution de ce compte.') : this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  async changeRole(user: UserSession, role: 'LIBRARIAN' | 'ADMIN'): Promise<void> {
    const accepted = await this.confirm.ask({
      title: this.i18n.t(role === 'ADMIN' ? 'Donner les droits d\'administration ?' : 'Retirer les droits d\'administration ?'),
      message: `${user.firstName} ${user.lastName} ${this.i18n.t(role === 'ADMIN'
        ? 'pourra gérer tous les comptes, les institutions et la configuration, mais ne pourra plus importer ni valider de documents.'
        : 'redeviendra bibliothécaire de son institution.')}`,
      action: this.i18n.t('Confirmer')
    });
    if (!accepted) { this.loadUsers(); return; }
    this.api.updateAdminUser(user.id, { role }).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Rôle modifié.')); this.loadUsers(); },
      error: () => this.toasts.show(this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  createInstitution(): void {
    const emailDomain = this.inst.emailDomain.trim().toLowerCase().replace(/^@/, '');
    this.api.createInstitution({ code: emailDomain.split('.')[0].toUpperCase(), name: this.inst.name.trim(), emailDomain }).subscribe({
      next: () => { this.inst = { name: '', emailDomain: '' }; this.toasts.show(this.i18n.t('Institution ajoutée.')); this.loadInstitutions(); },
      error: (e) => this.toasts.show(this.i18n.t(e?.status === 409 ? 'Une institution existe déjà avec ce nom ou ce domaine.' : 'La création a échoué.'), 'error')
    });
  }

  async deactivateInstitution(i: Institution): Promise<void> {
    const accepted = await this.confirm.ask({
      title: this.i18n.t('Désactiver cette institution ?'),
      message: `${i.name} ${this.i18n.t('n\'acceptera plus de nouvelles inscriptions. Ses publications restent dans le catalogue.')}`,
      action: this.i18n.t('Désactiver')
    });
    if (!accepted) return;
    this.api.deactivateInstitution(i.id).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Institution désactivée.')); this.loadInstitutions(); },
      error: () => this.toasts.show(this.i18n.t('La modification a échoué.'), 'error')
    });
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

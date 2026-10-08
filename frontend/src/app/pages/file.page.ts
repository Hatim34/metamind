import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService, Publication, PublicationStatus } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { STATUS_LABELS, typeLabel, VISIBILITY_LABELS } from '../core/labels';
import { SessionService } from '../core/session.service';
import { ToastService } from '../core/toast.service';
import { ConfirmService } from '../core/confirm.service';
import { TypeCoverComponent } from '../ui/type-cover.component';

/** Onglets de la file, dans l'ordre du cycle de vie d'un document (livrable 07). */
const TABS: { status: PublicationStatus; label: string }[] = [
  { status: 'A_VALIDER', label: 'À valider' },
  { status: 'EN_ATTENTE', label: 'Non extraits' },
  { status: 'EXTRACTION', label: 'En cours' },
  { status: 'REJETE', label: 'Rejetés' },
  { status: 'ECHEC', label: 'Fichiers illisibles' },
  { status: 'PUBLIE', label: 'Publiés' }
];

/** Liste des documents par statut (B4, B5, B8). Une seule action principale par ligne. */
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe, TypeCoverComponent],
  template: `
    <div class="m-wrap m-page">
      <div class="m-page__head">
        <h1 class="m-title">{{ (session.isAdmin() ? 'Documents' : 'File de validation') | t }}</h1>
        <div class="m-actions">
          @if (session.isAdmin()) {
            <select class="m-input m-input--sm" [(ngModel)]="institution" name="institution" [attr.aria-label]="'Institution' | t">
              <option value="">{{ 'Toutes les institutions' | t }}</option>
              @for (name of institutions(); track name) { <option [value]="name">{{ name }}</option> }
            </select>
          }
          <input class="m-input m-input--search" [(ngModel)]="filter" name="filtre" [attr.aria-label]="'Filtrer par titre' | t" [placeholder]="'Filtrer par titre' | t" />
        </div>
      </div>

      <div class="m-tabs" role="tablist" [attr.aria-label]="'Statut' | t">
        @for (tab of tabs; track tab.status) {
          <button type="button" role="tab" [attr.aria-selected]="status() === tab.status" [class.is-on]="status() === tab.status" (click)="select(tab.status)">
            {{ tab.label | t }} <span class="m-muted">{{ count(tab.status) }}</span>
          </button>
        }
      </div>

      @if (!session.isAdmin() && status() === 'EN_ATTENTE' && extractable().length) {
        <div class="m-callout">
          <span>{{ extractable().length }} {{ 'documents dont le texte est lu attendent l\\'analyse par l\\'IA.' | t }}</span>
          <button type="button" class="m-btn m-btn--primary m-btn--sm" [disabled]="running()" (click)="extractAll()">
            {{ running() ? ('Analyse en cours…' | t) : ('Tout extraire' | t) + ' (' + extractable().length + ' ' + ('crédits' | t) + ')' }}
          </button>
        </div>
      }

      <section class="m-sheet m-scroll">
        <table class="m-table">
          <thead><tr>
            <th>{{ 'Document' | t }}</th>
            @if (session.isAdmin()) { <th>{{ 'Institution' | t }}</th> }
            <th>{{ 'Visibilité' | t }}</th><th>{{ 'Statut' | t }}</th><th><span class="m-sr">{{ 'Actions' | t }}</span></th>
          </tr></thead>
          <tbody>
            @for (doc of visible(); track doc.id) {
              <tr>
                <td>
                  <span class="m-doc">
                    <m-type-cover [type]="doc.documentType" size="sm" [imageUrl]="doc.imageUrl" />
                    <span class="m-line__main"><strong class="m-ellipsis">{{ doc.title }}</strong><span class="m-muted m-small">{{ typeLabel(doc.documentType) | t }}@if (doc.year) {, {{ doc.year }}}</span></span>
                  </span>
                </td>
                @if (session.isAdmin()) { <td class="m-small">{{ doc.institution }}</td> }
                <td>{{ vis[doc.visibility] | t }}</td>
                <td><span [class]="'m-badge m-badge--' + doc.status">{{ labels[doc.status] | t }}</span></td>
                <td class="m-right">
                  <span class="m-actions m-actions--row">
                    @if (!session.isAdmin()) {
                      @switch (doc.status) {
                        @case ('A_VALIDER') { <a class="m-btn m-btn--primary m-btn--sm" [routerLink]="['/espace/documents', doc.id, 'validation']">{{ 'Valider' | t }}</a> }
                        @case ('EN_ATTENTE') {
                          @if (doc.textReady) { <button type="button" class="m-btn m-btn--primary m-btn--sm" [disabled]="busy().has(doc.id)" (click)="extract(doc)">{{ (busy().has(doc.id) ? 'Analyse…' : 'Extraire (1 crédit)') | t }}</button> }
                          @else { <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="retry(doc)">{{ 'Relancer la lecture' | t }}</button> }
                        }
                        @case ('EXTRACTION') { <span class="m-muted m-small">{{ (doc.textReady ? 'Analyse par l\\'IA…' : 'Lecture du fichier…') | t }}</span> }
                        @case ('REJETE') { <button type="button" class="m-btn m-btn--ghost m-btn--sm" [disabled]="busy().has(doc.id)" (click)="extract(doc)">{{ 'Relancer l\\'analyse (1 crédit)' | t }}</button> }
                        @case ('ECHEC') { <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="retry(doc)">{{ 'Relancer la lecture' | t }}</button> }
                        @case ('PUBLIE') { <a class="m-btn m-btn--ghost m-btn--sm" [routerLink]="['/espace/documents', doc.id, 'validation']">{{ 'Modifier' | t }}</a> }
                      }
                    }
                    @if (doc.status === 'PUBLIE') { <a class="m-btn m-btn--ghost m-btn--sm" [routerLink]="['/publications', doc.id]">{{ 'Voir la fiche' | t }}</a> }
                    <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="remove(doc)">{{ 'Supprimer' | t }}</button>
                  </span>
                </td>
              </tr>
            } @empty {
              <tr><td [attr.colspan]="session.isAdmin() ? 5 : 4" class="m-empty">{{ 'Aucun document dans cette catégorie.' | t }}</td></tr>
            }
          </tbody>
        </table>
      </section>
    </div>
  `
})
export class FilePage implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly i18n = inject(I18nService);
  readonly session = inject(SessionService);
  readonly labels = STATUS_LABELS;
  readonly vis = VISIBILITY_LABELS;
  readonly typeLabel = typeLabel;
  readonly tabs = TABS;

  filter = '';
  institution = '';
  readonly status = signal<PublicationStatus>('A_VALIDER');
  readonly docs = signal<Publication[]>([]);
  readonly busy = signal(new Set<number>());
  readonly running = signal(false);
  readonly institutions = computed(() => [...new Set(this.docs().map((d) => d.institution))].sort());
  readonly extractable = computed(() => this.docs().filter((d) => d.status === 'EN_ATTENTE' && d.textReady));
  private refresh?: ReturnType<typeof setInterval>;

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      const s = params.get('statut') as PublicationStatus | null;
      this.status.set(s && TABS.some((t) => t.status === s) ? s : 'A_VALIDER');
    });
    this.load();
    // Tant qu'un fichier est lu ou analysé, la file se met à jour seule.
    this.refresh = setInterval(() => {
      if (this.docs().some((d) => d.status === 'EXTRACTION' || (d.status === 'EN_ATTENTE' && !d.textReady))) this.load();
    }, 4000);
  }

  ngOnDestroy(): void {
    clearInterval(this.refresh);
  }

  count(status: PublicationStatus): number {
    return this.scoped().filter((d) => d.status === status).length;
  }

  visible(): Publication[] {
    const f = this.filter.trim().toLowerCase();
    return this.scoped()
      .filter((d) => d.status === this.status())
      .filter((d) => !f || d.title.toLowerCase().includes(f) || (d.author ?? '').toLowerCase().includes(f));
  }

  select(status: PublicationStatus): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { statut: status } });
  }

  /** Tous les statuts en une requête : les compteurs des onglets et le changement d'onglet sont immédiats. */
  load(): void {
    this.api.getManagedDocuments().subscribe({ next: (docs) => this.docs.set(docs.filter((d) => d.status !== 'SUPPRIME')), error: () => this.docs.set([]) });
  }

  extract(doc: Publication): void {
    this.busy.update((s) => new Set(s).add(doc.id));
    this.api.extractMetadataWhenReady(doc.id).subscribe({
      next: () => { this.done(doc); this.toasts.show(this.i18n.t('Notice prête à valider.')); this.load(); },
      error: (e) => {
        this.done(doc);
        this.toasts.show(this.i18n.t(e?.status === 402 ? 'Crédits insuffisants pour lancer l\'extraction.' : 'L\'extraction a échoué. Aucun crédit n\'a été consommé.'), 'error');
        this.load();
      }
    });
  }

  /** Analyse un document après l'autre : on s'arrête dès que les crédits manquent. */
  async extractAll(): Promise<void> {
    this.running.set(true);
    let ok = 0;
    for (const doc of this.extractable()) {
      this.busy.update((s) => new Set(s).add(doc.id));
      try {
        await firstValueFrom(this.api.extractMetadata(doc.id));
        ok++;
      } catch (e: any) {
        if (e?.status === 402) {
          this.toasts.show(this.i18n.t('Crédits insuffisants pour lancer l\'extraction.'), 'error');
          this.done(doc);
          break;
        }
      }
      this.done(doc);
      this.load();
    }
    this.running.set(false);
    this.load();
    if (ok) this.toasts.show(`${ok} ${this.i18n.t('notices prêtes à valider')}`);
  }

  retry(doc: Publication): void {
    this.api.retryDocumentProcessing(doc.id).subscribe({ next: () => this.load(), error: () => this.toasts.show(this.i18n.t('Le traitement n\'a pas pu être relancé.'), 'error') });
  }

  async remove(doc: Publication): Promise<void> {
    const accepted = await this.confirm.ask({
      title: this.i18n.t('Supprimer ce document ?'),
      message: `« ${doc.title || this.i18n.t('Sans titre')} » ${this.i18n.t(doc.status === 'PUBLIE' ? 'sera retiré du catalogue public et de la file.' : 'sera retiré de la file.')}`,
      action: this.i18n.t('Supprimer')
    });
    if (!accepted) return;
    this.api.deletePublication(doc.id).subscribe({ next: () => { this.toasts.show(this.i18n.t('Document supprimé.')); this.load(); }, error: () => this.toasts.show(this.i18n.t('La suppression a échoué.'), 'error') });
  }

  private scoped(): Publication[] {
    return this.institution ? this.docs().filter((d) => d.institution === this.institution) : this.docs();
  }

  private done(doc: Publication): void {
    this.busy.update((s) => { const n = new Set(s); n.delete(doc.id); return n; });
  }
}

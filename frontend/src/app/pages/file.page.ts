import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, Publication, PublicationStatus } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { STATUS_LABELS, typeLabel, VISIBILITY_LABELS } from '../core/labels';
import { ToastService } from '../core/toast.service';
import { TypeCoverComponent } from '../ui/type-cover.component';

/** Liste des documents de l'institution par statut (B4, B5, B8). Une seule action principale par ligne. */
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe, TypeCoverComponent],
  template: `
    <div class="m-wrap m-page">
      <div class="m-page__head">
        <h1 class="m-title">{{ 'File de validation' | t }}</h1>
        <input class="m-input m-input--search" [(ngModel)]="filter" name="filtre" [attr.aria-label]="'Filtrer par titre' | t" [placeholder]="'Filtrer par titre' | t" />
      </div>

      <div class="m-tabs" role="tablist" [attr.aria-label]="'Statut' | t">
        @for (tab of tabs; track tab) {
          <button type="button" role="tab" [attr.aria-selected]="status() === tab" [class.is-on]="status() === tab" (click)="select(tab)">{{ labels[tab] | t }}</button>
        }
      </div>

      <section class="m-sheet m-scroll">
        <table class="m-table">
          <thead><tr><th>{{ 'Document' | t }}</th><th>{{ 'Visibilité' | t }}</th><th>{{ 'Statut' | t }}</th><th><span class="m-sr">{{ 'Actions' | t }}</span></th></tr></thead>
          <tbody>
            @for (doc of visible(); track doc.id) {
              <tr>
                <td>
                  <span class="m-doc">
                    <m-type-cover [type]="doc.documentType" size="sm" [imageUrl]="doc.imageUrl" />
                    <span class="m-line__main"><strong class="m-ellipsis">{{ doc.title }}</strong><span class="m-muted m-small">{{ typeLabel(doc.documentType) | t }}@if (doc.year) {, {{ doc.year }}}</span></span>
                  </span>
                </td>
                <td>{{ vis[doc.visibility] | t }}</td>
                <td><span [class]="'m-badge m-badge--' + doc.status">{{ labels[doc.status] | t }}</span></td>
                <td class="m-right">
                  <span class="m-actions m-actions--row">
                    @switch (doc.status) {
                      @case ('A_VALIDER') { <a class="m-btn m-btn--primary m-btn--sm" [routerLink]="['/espace/documents', doc.id, 'validation']">{{ 'Valider' | t }}</a> }
                      @case ('EN_ATTENTE') { <button type="button" class="m-btn m-btn--primary m-btn--sm" [disabled]="busy().has(doc.id)" (click)="extract(doc)">{{ 'Extraire (1 crédit)' | t }}</button> }
                      @case ('PUBLIE') { <a class="m-btn m-btn--ghost m-btn--sm" [routerLink]="['/publications', doc.id]">{{ 'Voir la fiche' | t }}</a> }
                      @case ('EXTRACTION') { <span class="m-muted m-small">{{ 'En cours…' | t }}</span> }
                    }
                    <details class="m-menu m-menu--end">
                      <summary class="m-btn m-btn--ghost m-btn--icon" [attr.aria-label]="'Plus d\\'actions' | t">⋯</summary>
                      <div class="m-menu__panel">
                        @if (doc.status === 'EN_ATTENTE') { <button type="button" (click)="retry(doc)">{{ 'Relancer le traitement du fichier' | t }}</button> }
                        <button type="button" class="is-danger" (click)="remove(doc)">{{ 'Supprimer' | t }}</button>
                      </div>
                    </details>
                  </span>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="m-empty">{{ 'Aucun document dans cette catégorie.' | t }}</td></tr>
            }
          </tbody>
        </table>
      </section>
    </div>
  `
})
export class FilePage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);
  readonly labels = STATUS_LABELS;
  readonly vis = VISIBILITY_LABELS;
  readonly typeLabel = typeLabel;
  readonly tabs: PublicationStatus[] = ['A_VALIDER', 'EXTRACTION', 'EN_ATTENTE', 'PUBLIE'];

  filter = '';
  readonly status = signal<PublicationStatus>('A_VALIDER');
  readonly docs = signal<Publication[]>([]);
  readonly busy = signal(new Set<number>());

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      const s = params.get('statut') as PublicationStatus | null;
      this.status.set(s && this.tabs.includes(s) ? s : 'A_VALIDER');
      this.load();
    });
  }

  visible(): Publication[] {
    const f = this.filter.trim().toLowerCase();
    return f ? this.docs().filter((d) => d.title.toLowerCase().includes(f) || (d.author ?? '').toLowerCase().includes(f)) : this.docs();
  }

  select(tab: PublicationStatus): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { statut: tab } });
  }

  load(): void {
    this.api.getManagedDocuments(this.status()).subscribe({ next: (docs) => this.docs.set(docs), error: () => this.docs.set([]) });
  }

  extract(doc: Publication): void {
    this.busy.update((s) => new Set(s).add(doc.id));
    this.api.extractMetadataWhenReady(doc.id).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Extraction lancée.')); this.load(); },
      error: (e) => {
        this.busy.update((s) => { const n = new Set(s); n.delete(doc.id); return n; });
        this.toasts.show(this.i18n.t(e?.status === 402 ? 'Crédits insuffisants pour lancer l\'extraction.' : 'L\'extraction a échoué. Aucun crédit n\'a été consommé.'), 'error');
      }
    });
  }

  retry(doc: Publication): void {
    this.api.retryDocumentProcessing(doc.id).subscribe({ next: () => this.load(), error: () => this.toasts.show(this.i18n.t('Le traitement n\'a pas pu être relancé.'), 'error') });
  }

  remove(doc: Publication): void {
    if (!confirm(this.i18n.t('Supprimer ce document ? Il sera archivé (suppression logique).'))) return;
    this.api.deletePublication(doc.id).subscribe({ next: () => { this.toasts.show(this.i18n.t('Document supprimé.')); this.load(); }, error: () => this.toasts.show(this.i18n.t('La suppression a échoué.'), 'error') });
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, DashboardStatistics, ExtractionQuality, Publication } from '../api.service';
import { TranslatePipe } from '../core/i18n';
import { SessionService } from '../core/session.service';
import { typeLabel } from '../core/labels';
import { TypeCoverComponent } from '../ui/type-cover.component';

/** Champs mesurés : libellés affichés dans le bloc qualité. */
const FIELD_LABELS: Record<string, string> = {
  titre: 'Titre', auteurs: 'Auteurs', resume: 'Résumé', mots_cles: 'Mots-clés', date_publication: 'Date',
  doi: 'DOI', langue: 'Langue', type_document: 'Type de document', classification: 'Classification'
};

/** Tableau de bord du bibliothécaire (B4, B7, B10). */
@Component({
  standalone: true,
  imports: [RouterLink, DecimalPipe, TranslatePipe, TypeCoverComponent],
  template: `
    <div class="m-wrap m-page">
      <div class="m-page__head">
        <div>
          <h1 class="m-title">{{ 'Bonjour' | t }} {{ session.user()?.firstName }}</h1>
          <p class="m-muted">
            @if (queue().length) { {{ queue().length }} {{ (session.isAdmin() ? 'notices attendent une relecture dans les institutions.' : 'notices attendent votre relecture.') | t }} }
            @else { {{ 'Aucune notice en attente de relecture.' | t }} }
          </p>
        </div>
        @if (!session.isAdmin()) {
          <div class="m-actions">
            <a class="m-btn m-btn--ghost" routerLink="/espace/import">{{ 'Importer des documents' | t }}</a>
            @if (queue().length) { <a class="m-btn m-btn--primary" [routerLink]="['/espace/documents', queue()[0].id, 'validation']">{{ 'Reprendre la validation' | t }}</a> }
          </div>
        }
      </div>

      @if (session.isAdmin() && adminRequests()) {
        <a class="m-banner m-banner--link" routerLink="/admin">{{ adminRequests() }} {{ 'demandes de compte ou d\'institution attendent votre décision.' | t }}</a>
      }

      @if (stats(); as s) {
        <div class="m-kpis">
          <div class="m-kpi"><span>{{ 'À valider' | t }}</span><strong>{{ s.pendingValidationPublications }}</strong></div>
          <div class="m-kpi"><span>{{ 'Publiées' | t }}</span><strong>{{ s.publishedPublications }}</strong><small>{{ 'dont' | t }} {{ s.publicPublications }} {{ 'publiques' | t }}, {{ s.institutionOnlyPublications }} {{ 'réservées' | t }}</small></div>
          <div class="m-kpi"><span>{{ (session.isAdmin() ? 'Crédits, toutes institutions' : 'Crédits restants') | t }}</span><strong>{{ s.creditBalance }}</strong><small>@if (session.isAdmin()) { <a routerLink="/admin">{{ 'Ajuster dans l\'administration' | t }}</a> } @else { <a routerLink="/espace/credits">{{ 'Acheter des crédits' | t }}</a> }</small></div>
          <div class="m-kpi"><span>{{ 'Délai moyen de relecture' | t }}</span>
            <strong>{{ s.averageProcessingHours != null ? (s.averageProcessingHours | number: '1.0-1') + ' h' : '–' }}</strong>
            <small>{{ 'entre la proposition de l\\'IA et la validation' | t }}</small>
          </div>
          <div class="m-kpi"><span>{{ 'Taux de validation' | t }}</span>
            <strong>{{ (s.validationRate ?? 0) | number: '1.0-0' }} %</strong>
            <small>{{ 'des documents publiés' | t }} · {{ 'rejet' | t }} {{ (s.rejectionRate ?? 0) | number: '1.0-0' }} %</small>
          </div>
        </div>
      }

      <div class="m-split m-split--wide">
        <section class="m-sheet">
          <div class="m-sheet__head"><h2 class="m-h3">{{ (session.isAdmin() ? 'En attente de relecture' : 'À valider en priorité') | t }}</h2><a routerLink="/espace/file">{{ 'Toute la file' | t }}</a></div>
          @for (doc of queue().slice(0, 6); track doc.id) {
            <a class="m-line" [routerLink]="session.isAdmin() ? ['/espace/file'] : ['/espace/documents', doc.id, 'validation']">
              <m-type-cover [type]="doc.documentType" size="sm" [imageUrl]="doc.imageUrl" />
              <span class="m-line__main"><strong class="m-ellipsis">{{ doc.title }}</strong><span class="m-muted m-small">{{ typeLabel(doc.documentType) | t }}@if (doc.year) {, {{ doc.year }}}@if (session.isAdmin()) { · {{ doc.institution }}}</span></span>
              @if (!session.isAdmin()) { <span class="m-btn m-btn--primary m-btn--sm">{{ 'Valider' | t }}</span> }
            </a>
          } @empty {
            <p class="m-empty">{{ 'Rien à valider. Importez de nouveaux documents pour remplir la file.' | t }}</p>
          }
        </section>

        <section class="m-sheet m-sheet--pad">
          <h2 class="m-h4">{{ 'Répartition par type de document' | t }}</h2>
          @for (row of types(); track row.label) {
            <div class="m-barrow">
              <span>{{ row.label | t }}</span>
              <span class="m-barrow__track"><span class="m-barrow__fill" [style.width.%]="row.pct"></span></span>
              <span class="m-num">{{ row.count }}</span>
            </div>
          } @empty {
            <p class="m-muted m-small">{{ 'Pas encore assez de données.' | t }}</p>
          }
        </section>
      </div>

      <section class="m-sheet m-sheet--pad m-mt">
        <div class="m-row-between">
          <h2 class="m-h3">{{ 'Qualité des suggestions de l\\'IA' | t }}</h2>
          @if (quality(); as q) { <span class="m-muted m-small">{{ q.suggestions_arbitrees }} {{ 'champs arbitrés par les bibliothécaires' | t }}</span> }
        </div>
        @if (quality(); as q) {
          @if (q.suggestions_arbitrees) {
            <p class="m-muted m-small">{{ 'Part des propositions de l\\'IA publiées sans modification, par champ.' | t }}</p>
            <div class="m-quality">
              <div>
                @for (row of fields(); track row.champ) {
                  <div class="m-barrow">
                    <span>{{ row.label | t }}</span>
                    <span class="m-barrow__track"><span class="m-barrow__fill" [style.width.%]="row.taux_acceptation"></span></span>
                    <span class="m-num">{{ row.taux_acceptation | number: '1.0-0' }} %</span>
                  </div>
                }
              </div>
              <div>
                <h3 class="m-h4">{{ 'Le score de confiance est-il fiable ?' | t }}</h3>
                <p class="m-muted m-small">{{ 'Taux d\\'acceptation selon la confiance annoncée : un score fiable donne plus d\\'acceptations quand il est élevé.' | t }}</p>
                @for (bucket of q.calibration_score; track bucket.tranche) {
                  <div class="m-barrow">
                    <span>{{ 'Confiance' | t }} {{ bucket.tranche }}</span>
                    <span class="m-barrow__track"><span class="m-barrow__fill" [style.width.%]="bucket.taux_acceptation"></span></span>
                    <span class="m-num">{{ bucket.taux_acceptation | number: '1.0-0' }} %</span>
                  </div>
                }
              </div>
            </div>
          } @else {
            <p class="m-muted m-small">{{ 'Aucune notice validée pour le moment : le taux d\\'acceptation par champ apparaîtra après les premières validations.' | t }}</p>
          }
        }
      </section>
    </div>
  `
})
export class EspacePage {
  private readonly api = inject(ApiService);
  readonly session = inject(SessionService);
  readonly typeLabel = typeLabel;
  readonly stats = signal<DashboardStatistics | null>(null);
  readonly queue = signal<Publication[]>([]);
  readonly quality = signal<ExtractionQuality | null>(null);
  /** Comptes et institutions en attente d'une décision de l'administrateur. */
  readonly adminRequests = signal(0);
  readonly fields = computed(() => (this.quality()?.par_champ ?? [])
    .filter((row) => FIELD_LABELS[row.champ])
    .map((row) => ({ ...row, label: FIELD_LABELS[row.champ] }))
    .sort((a, b) => b.taux_acceptation - a.taux_acceptation));
  readonly types = computed(() => {
    const dist = this.stats()?.documentTypeDistribution ?? {};
    const total = Object.values(dist).reduce((a, b) => a + b, 0) || 1;
    return Object.entries(dist).sort((a, b) => b[1] - a[1]).map(([label, count]) => ({ label: typeLabel(label), count, pct: Math.round((count / total) * 100) }));
  });

  constructor() {
    this.api.getStatistics().subscribe({ next: (s) => this.stats.set(s), error: () => undefined });
    this.api.getManagedDocuments('A_VALIDER').subscribe({ next: (docs) => this.queue.set(docs), error: () => undefined });
    this.api.getExtractionQuality().subscribe({ next: (q) => this.quality.set(q), error: () => undefined });
    if (this.session.isAdmin()) {
      this.api.getAdminUsers().subscribe({ next: (users) => this.adminRequests.set(users.filter((u) => u.status === 'EN_ATTENTE').length), error: () => undefined });
    }
  }
}

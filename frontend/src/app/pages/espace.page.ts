import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, DashboardStatistics, Publication } from '../api.service';
import { TranslatePipe } from '../core/i18n';
import { SessionService } from '../core/session.service';
import { typeLabel } from '../core/labels';
import { TypeCoverComponent } from '../ui/type-cover.component';

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
            @if (queue().length) { {{ queue().length }} {{ 'notices attendent votre relecture.' | t }} }
            @else { {{ 'Aucune notice en attente de relecture.' | t }} }
          </p>
        </div>
        <div class="m-actions">
          <a class="m-btn m-btn--ghost" routerLink="/espace/import">{{ 'Importer des documents' | t }}</a>
          @if (queue().length) { <a class="m-btn m-btn--primary" [routerLink]="['/espace/documents', queue()[0].id, 'validation']">{{ 'Reprendre la validation' | t }}</a> }
        </div>
      </div>

      @if (stats(); as s) {
        <div class="m-kpis">
          <div class="m-kpi"><span>{{ 'À valider' | t }}</span><strong>{{ s.pendingValidationPublications }}</strong></div>
          <div class="m-kpi"><span>{{ 'Publiées' | t }}</span><strong>{{ s.publishedPublications }}</strong><small>{{ s.publicPublications }} {{ 'publiques' | t }}, {{ s.institutionOnlyPublications }} {{ 'réservées' | t }}</small></div>
          <div class="m-kpi"><span>{{ 'Crédits restants' | t }}</span><strong>{{ s.creditBalance }}</strong><small><a routerLink="/espace/credits">{{ 'Acheter des crédits' | t }}</a></small></div>
          <div class="m-kpi"><span>{{ 'Temps moyen de traitement' | t }}</span>
            <strong>{{ s.averageProcessingHours != null ? (s.averageProcessingHours | number: '1.0-1') + ' h' : '–' }}</strong>
            @if (s.validationRate != null) { <small>{{ 'Taux de validation' | t }} {{ s.validationRate | number: '1.0-0' }} %</small> }
          </div>
        </div>
      }

      <div class="m-split m-split--wide">
        <section class="m-sheet">
          <div class="m-sheet__head"><h2 class="m-h3">{{ 'À valider en priorité' | t }}</h2><a routerLink="/espace/file">{{ 'Toute la file' | t }}</a></div>
          @for (doc of queue().slice(0, 6); track doc.id) {
            <a class="m-line" [routerLink]="['/espace/documents', doc.id, 'validation']">
              <m-type-cover [type]="doc.documentType" size="sm" />
              <span class="m-line__main"><strong class="m-ellipsis">{{ doc.title }}</strong><span class="m-muted m-small">{{ typeLabel(doc.documentType) | t }}, {{ doc.year }}</span></span>
              <span class="m-btn m-btn--primary m-btn--sm">{{ 'Valider' | t }}</span>
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
          <h2 class="m-h4 m-mt">{{ 'Qualité des suggestions de l\\'IA' | t }}</h2>
          <p class="m-muted m-small">{{ 'Le taux d\\'acceptation par champ s\\'affichera dès que les décisions de validation seront enregistrées.' | t }}</p>
        </section>
      </div>
    </div>
  `
})
export class EspacePage {
  private readonly api = inject(ApiService);
  readonly session = inject(SessionService);
  readonly typeLabel = typeLabel;
  readonly stats = signal<DashboardStatistics | null>(null);
  readonly queue = signal<Publication[]>([]);
  readonly types = computed(() => {
    const dist = this.stats()?.documentTypeDistribution ?? {};
    const total = Object.values(dist).reduce((a, b) => a + b, 0) || 1;
    return Object.entries(dist).sort((a, b) => b[1] - a[1]).map(([label, count]) => ({ label: typeLabel(label), count, pct: Math.round((count / total) * 100) }));
  });

  constructor() {
    this.api.getStatistics().subscribe({ next: (s) => this.stats.set(s), error: () => undefined });
    this.api.getManagedDocuments('A_VALIDER').subscribe({ next: (docs) => this.queue.set(docs), error: () => undefined });
  }
}

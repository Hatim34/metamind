import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, Publication } from '../api.service';
import { TranslatePipe } from '../core/i18n';
import { coverKind, languageLabel, typeLabel } from '../core/labels';
import { TypeCoverComponent } from '../ui/type-cover.component';

interface Facet { key: string; label: string; count: number; on: boolean; }

/**
 * Catalogue public (C1) : recherche, filtres auteur / date / type / langue (+ institution), tri, pagination.
 * L'API actuelle renvoie une liste sans facettes ni pagination : on les calcule ici en attendant
 * l'endpoint /search paginé avec facettes (prompt 7).
 */
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe, TypeCoverComponent],
  template: `
    <div class="m-wrap m-wrap--public m-page">
      <form class="m-search m-search--bar" role="search" (ngSubmit)="apply()">
        <label class="m-sr" for="cq">{{ 'Rechercher' | t }}</label>
        <input id="cq" name="q" class="m-input" [(ngModel)]="q" [placeholder]="'Titre, auteur, mot-clé…' | t" />
        <button class="m-btn m-btn--primary" type="submit">{{ 'Rechercher' | t }}</button>
      </form>

      <div class="m-split">
        <aside class="m-facets" [attr.aria-label]="'Filtres' | t">
          <fieldset class="m-facet">
            <legend>{{ 'Auteur' | t }}</legend>
            <input class="m-input" name="auteur" [(ngModel)]="author" (change)="apply()" [placeholder]="'Nom de l\\'auteur' | t" />
          </fieldset>
          <fieldset class="m-facet">
            <legend>{{ 'Date de publication' | t }}</legend>
            <div class="m-inline">
              <label class="m-sr" for="du">{{ 'De' | t }}</label>
              <input id="du" class="m-input" type="number" min="1900" max="2100" [(ngModel)]="yearFrom" name="du" (change)="apply()" [placeholder]="'De' | t" />
              <label class="m-sr" for="au">{{ 'À' | t }}</label>
              <input id="au" class="m-input" type="number" min="1900" max="2100" [(ngModel)]="yearTo" name="au" (change)="apply()" [placeholder]="'À' | t" />
            </div>
          </fieldset>
          @for (group of facetGroups(); track group.name) {
            @if (group.items.length) {
              <fieldset class="m-facet">
                <legend>{{ group.name | t }}</legend>
                @for (item of group.items; track item.key) {
                  <label class="m-check">
                    <input type="checkbox" [checked]="item.on" (change)="toggle(group.param, item.key)" />
                    <span>{{ item.label | t }}</span>
                    <span class="m-muted">{{ item.count }}</span>
                  </label>
                }
              </fieldset>
            }
          }
          @if (hasFilters()) {
            <button type="button" class="m-btn m-btn--ghost" (click)="reset()">{{ 'Effacer les filtres' | t }}</button>
          }
        </aside>

        <section class="m-results" [attr.aria-label]="'Résultats' | t">
          <div class="m-results__head">
            <p><strong>{{ filtered().length }} {{ 'résultats' | t }}</strong>
              @if (q) { <span class="m-muted"> {{ 'pour' | t }} « {{ q }} »</span> }</p>
            <label class="m-inline m-small">{{ 'Trier par' | t }}
              <select class="m-input m-input--sm" [ngModel]="sort()" name="tri" (ngModelChange)="sort.set($event); page.set(0)">
                <option value="pertinence">{{ 'Pertinence' | t }}</option>
                <option value="recent">{{ 'Plus récent' | t }}</option>
                <option value="titre">{{ 'Titre' | t }}</option>
              </select>
            </label>
          </div>

          @if (loading()) {
            <p class="m-muted">{{ 'Chargement…' | t }}</p>
          } @else if (filtered().length === 0) {
            <div class="m-empty">
              <p>{{ 'Aucune publication ne correspond à cette recherche.' | t }}</p>
              <button type="button" class="m-btn m-btn--ghost" (click)="reset()">{{ 'Effacer les filtres' | t }}</button>
            </div>
          } @else {
            @for (p of pageItems(); track p.id) {
              <article class="m-result">
                <m-type-cover [type]="p.documentType" [year]="p.year" [imageUrl]="p.imageUrl" />
                <div class="m-result__body">
                  <span class="m-muted m-small">{{ typeLabel(p.documentType) | t }}, {{ p.year }}, {{ p.institution }}</span>
                  <a class="m-result__title" [routerLink]="['/publications', p.id]">{{ p.title }}</a>
                  @if (p.author) { <span>{{ p.author }}</span> }
                  @if (p.summary) { <p class="m-result__snippet">{{ excerpt(p.summary) }}</p> }
                  <span class="m-result__meta">
                    @if (p.language) { <span>{{ languageLabel(p.language) | t }}</span> }
                    @for (k of p.keywords.slice(0, 3); track k) { <span class="m-chip m-chip--quiet">{{ k }}</span> }
                    <a [routerLink]="['/publications', p.id]" fragment="citer">{{ 'Voir la référence' | t }}</a>
                  </span>
                </div>
              </article>
            }
            @if (pages() > 1) {
              <nav class="m-pager" [attr.aria-label]="'Pagination' | t">
                <button type="button" class="m-btn m-btn--ghost" [disabled]="page() === 0" (click)="page.set(page() - 1)">{{ 'Précédent' | t }}</button>
                <span>{{ page() + 1 }} / {{ pages() }}</span>
                <button type="button" class="m-btn m-btn--ghost" [disabled]="page() + 1 >= pages()" (click)="page.set(page() + 1)">{{ 'Suivant' | t }}</button>
              </nav>
            }
          }
        </section>
      </div>
    </div>
  `
})
export class CataloguePage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly typeLabel = typeLabel;
  readonly languageLabel = languageLabel;
  private readonly pageSize = 10;

  q = '';
  author = '';
  yearFrom: number | null = null;
  yearTo: number | null = null;
  readonly sort = signal('pertinence');
  readonly selected = signal<Record<string, string[]>>({ type: [], langue: [], institution: [] });
  readonly loading = signal(true);
  readonly items = signal<Publication[]>([]);
  readonly page = signal(0);

  readonly filtered = computed(() => {
    const sel = this.selected();
    const list = this.items().filter((p) =>
      (!sel['type'].length || sel['type'].includes(coverKind(p.documentType))) &&
      (!sel['langue'].length || sel['langue'].includes((p.language ?? '').toLowerCase())) &&
      (!sel['institution'].length || sel['institution'].includes(p.institution)));
    if (this.sort() === 'recent') return [...list].sort((a, b) => (b.year ?? 0) - (a.year ?? 0));
    if (this.sort() === 'titre') return [...list].sort((a, b) => a.title.localeCompare(b.title));
    return list;
  });
  readonly pages = computed(() => Math.ceil(this.filtered().length / this.pageSize));
  readonly pageItems = computed(() => this.filtered().slice(this.page() * this.pageSize, (this.page() + 1) * this.pageSize));
  readonly facetGroups = computed(() => {
    const sel = this.selected();
    const count = (param: string, keyOf: (p: Publication) => string, labelOf: (k: string) => string): Facet[] => {
      const counts = new Map<string, number>();
      this.items().forEach((p) => { const k = keyOf(p); if (k) counts.set(k, (counts.get(k) ?? 0) + 1); });
      return [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 8)
        .map(([key, n]) => ({ key, label: labelOf(key), count: n, on: sel[param].includes(key) }));
    };
    return [
      { name: 'Type de document', param: 'type', items: count('type', (p) => coverKind(p.documentType), (k) => (k === 'autre' ? 'Document' : typeLabel(k))) },
      { name: 'Langue', param: 'langue', items: count('langue', (p) => (p.language ?? '').toLowerCase(), languageLabel) },
      { name: 'Institution', param: 'institution', items: count('institution', (p) => p.institution, (k) => k) }
    ];
  });
  readonly hasFilters = computed(() => Object.values(this.selected()).some((v) => v.length) || !!this.author || !!this.yearFrom || !!this.yearTo);

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      this.q = params.get('q') ?? '';
      this.author = params.get('auteur') ?? '';
      this.yearFrom = params.get('du') ? Number(params.get('du')) : null;
      this.yearTo = params.get('au') ? Number(params.get('au')) : null;
      this.selected.set({ type: params.getAll('type'), langue: params.getAll('langue'), institution: params.getAll('institution') });
      this.page.set(0);
      this.load();
    });
  }

  load(): void {
    this.loading.set(true);
    this.api.searchPublications(this.q, {
      author: this.author || undefined,
      startDate: this.yearFrom ? `${this.yearFrom}-01-01` : undefined,
      endDate: this.yearTo ? `${this.yearTo}-12-31` : undefined
    }).subscribe({
      next: (items) => { this.items.set(items); this.loading.set(false); },
      error: () => { this.items.set([]); this.loading.set(false); }
    });
  }

  apply(): void {
    const sel = this.selected();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { q: this.q || null, auteur: this.author || null, du: this.yearFrom || null, au: this.yearTo || null,
        type: sel['type'], langue: sel['langue'], institution: sel['institution'] }
    });
  }

  toggle(param: string, key: string): void {
    this.selected.update((sel) => ({ ...sel, [param]: sel[param].includes(key) ? sel[param].filter((k) => k !== key) : [...sel[param], key] }));
    this.apply();
  }

  reset(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { q: this.q || null } });
  }

  excerpt(text: string): string {
    return text.length > 260 ? text.slice(0, 257).trimEnd() + '…' : text;
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, Publication } from '../api.service';
import { TranslatePipe } from '../core/i18n';
import { institutionPhoto } from '../core/photos';
import { TypeCoverComponent } from '../ui/type-cover.component';

@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe, TypeCoverComponent],
  template: `
    <section class="m-hero">
      <div class="m-wrap m-wrap--public m-hero__in">
        <div class="m-hero__copy">
          <h1 class="m-display">{{ 'Le savoir mieux rangé' | t }}</h1>
          <p class="m-lead">{{ 'Articles, thèses et rapports en libre accès. Chaque notice est proposée par une IA puis relue par un bibliothécaire de l\\'institution.' | t }}</p>
          <form class="m-search" role="search" (ngSubmit)="search()">
            <label class="m-sr" for="q">{{ 'Rechercher dans le catalogue' | t }}</label>
            <input id="q" name="q" class="m-input m-input--lg" [(ngModel)]="query" [placeholder]="'Titre, auteur, mot-clé…' | t" />
            <button class="m-btn m-btn--primary m-btn--lg" type="submit">{{ 'Rechercher' | t }}</button>
          </form>
          <div class="m-quick">
            <a routerLink="/catalogue" [queryParams]="{ type: 'these' }">{{ 'Thèses' | t }}</a>
            <a routerLink="/catalogue" [queryParams]="{ type: 'article' }">{{ 'Articles' | t }}</a>
            <a routerLink="/catalogue" [queryParams]="{ type: 'rapport' }">{{ 'Rapports' | t }}</a>
            <a routerLink="/catalogue" [queryParams]="{ langue: 'nl' }">{{ 'En néerlandais' | t }}</a>
          </div>
        </div>
        <div class="m-hero__art" aria-hidden="true"></div>
      </div>
      <div class="m-wrap m-wrap--public m-figures">
        <div class="m-figure"><strong>{{ publications().length }}</strong><span>{{ 'publications en libre accès' | t }}</span></div>
        <div class="m-figure"><strong>{{ institutions().length }}</strong><span>{{ 'institutions' | t }}</span></div>
        <div class="m-figure"><strong>{{ languages() }}</strong><span>{{ 'langues' | t }}</span></div>
      </div>
    </section>

    <section class="m-wrap m-wrap--public m-section">
      <div class="m-section__head">
        <h2 class="m-h2">{{ 'Dernières publications' | t }}</h2>
        <a routerLink="/catalogue">{{ 'Tout le catalogue' | t }}</a>
      </div>
      @if (loading()) {
        <p class="m-muted">{{ 'Chargement…' | t }}</p>
      } @else if (latest().length === 0) {
        <p class="m-empty">{{ 'Aucune publication pour le moment.' | t }}</p>
      } @else {
        <div class="m-cards">
          @for (p of latest(); track p.id) {
            <a class="m-card" [routerLink]="['/publications', p.id]">
              <m-type-cover [type]="p.documentType" [year]="p.year" [imageUrl]="p.imageUrl" />
              <span class="m-card__body">
                <span class="m-card__title">{{ p.title }}</span>
                <span class="m-muted m-small">{{ p.institution }}</span>
              </span>
            </a>
          }
        </div>
      }
    </section>

    @if (institutions().length) {
      <section class="m-wrap m-wrap--public m-section">
        <h2 class="m-h2">{{ 'Institutions' | t }}</h2>
        <div class="m-cards">
          @for (inst of institutions(); track inst.name) {
            <div class="m-inst">
              <a class="m-inst__link" routerLink="/catalogue" [queryParams]="{ institution: inst.name }">
                <span class="m-inst__band" aria-hidden="true">
                  @if (inst.photo) { <img [src]="inst.photo.src" alt="" loading="lazy" /> }
                </span>
                <span class="m-inst__body"><strong>{{ inst.name }}</strong><span class="m-muted">{{ inst.count }}</span></span>
              </a>
              @if (inst.photo) {
                <p class="m-inst__credit">{{ 'Photo' | t }} : <a [href]="inst.photo.source" target="_blank" rel="noopener">{{ inst.photo.credit }}</a></p>
              }
            </div>
          }
        </div>
      </section>
    }
  `
})
export class AccueilPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  query = '';
  readonly loading = signal(true);
  readonly publications = signal<Publication[]>([]);
  readonly latest = computed(() => [...this.publications()].sort((a, b) => (b.year ?? 0) - (a.year ?? 0)).slice(0, 8));
  readonly institutions = computed(() => {
    const counts = new Map<string, number>();
    this.publications().forEach((p) => counts.set(p.institution, (counts.get(p.institution) ?? 0) + 1));
    return [...counts.entries()]
      .map(([name, count]) => ({ name, count, photo: institutionPhoto(name) }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 8);
  });
  readonly languages = computed(() => new Set(this.publications().map((p) => p.language).filter(Boolean)).size);

  constructor() {
    this.api.searchPublications('').subscribe({
      next: (items) => { this.publications.set(items); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  search(): void {
    this.router.navigate(['/catalogue'], { queryParams: { q: this.query.trim() || null } });
  }
}

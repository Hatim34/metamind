import { Component, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiService, Publication, PublicationTranslation } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { languageLabel, typeLabel } from '../core/labels';
import { TypeCoverComponent } from '../ui/type-cover.component';
import { ToastService } from '../core/toast.service';

type CitationFormat = 'apa' | 'bibtex' | 'ris';

/** Fiche publication (C2) : notice complète, lecture du PDF en ligne, téléchargement, citation. */
@Component({
  standalone: true,
  imports: [RouterLink, TranslatePipe, TypeCoverComponent],
  template: `
    <div class="m-wrap m-wrap--public m-page">
      @if (pub(); as p) {
        <nav class="m-crumbs m-small" [attr.aria-label]="'Fil d\\'Ariane' | t">
          <a routerLink="/catalogue">{{ 'Catalogue' | t }}</a> / <span>{{ p.institution }}</span>
        </nav>
        <div class="m-split m-split--fiche">
          <aside class="m-fiche__side">
            <m-type-cover [type]="p.documentType" [year]="p.year" size="lg" [imageUrl]="coverUrl()" />
            @if (p.fileUrl !== null) {
              <button type="button" class="m-btn m-btn--primary m-btn--block" (click)="openReader()">{{ 'Lire en ligne' | t }}</button>
              <button type="button" class="m-btn m-btn--ghost m-btn--block" (click)="download()">{{ 'Télécharger le PDF' | t }}</button>
            }
          </aside>

          <article class="m-fiche">
            <h1 class="m-title">{{ display().title }}</h1>
            @if (p.author) { <p class="m-fiche__authors">{{ p.author }}</p> }
			@if (translation() && translation()!.translated) {
			  <p class="m-muted m-small">{{ 'Traduction automatique de la notice. La citation reste dans la langue originale.' | t }}</p>
			}

            <dl class="m-notice">
              @if (p.publicationDate || p.year) { <dt>{{ 'Date' | t }}</dt><dd>{{ displayDate(p) }}</dd> }
              @if (p.documentType) { <dt>{{ 'Type' | t }}</dt><dd>{{ typeLabel(p.documentType) | t }}</dd> }
              @if (p.language) { <dt>{{ 'Langue' | t }}</dt><dd>{{ languageLabel(p.language) | t }}</dd> }
              <dt>{{ 'Institution' | t }}</dt><dd><a routerLink="/catalogue" [queryParams]="{ institution: p.institution }">{{ p.institution }}</a></dd>
              @if (p.classification) { <dt>{{ 'Classification' | t }}</dt><dd>{{ p.classification }}</dd> }
              @if (display().keywords.length) {
                <dt>{{ 'Mots-clés' | t }}</dt>
                <dd class="m-chips">@for (k of display().keywords; track k) { <a class="m-chip" routerLink="/catalogue" [queryParams]="{ q: k }">{{ k }}</a> }</dd>
              }
            </dl>

            @if (display().summary) {
              <section>
                <h2 class="m-h3">{{ 'Résumé' | t }}</h2>
                <p class="m-prose">{{ display().summary }}</p>
              </section>
            }

            @if (readerUrl()) {
              <section class="m-reader">
                <div class="m-reader__bar">
                  <strong>{{ 'Lecture en ligne' | t }}</strong>
                  <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="closeReader()">{{ 'Fermer' | t }}</button>
                </div>
                <iframe [src]="readerUrl()" [title]="p.title"></iframe>
              </section>
            }

            <section id="citer" class="m-sheet m-sheet--pad">
              <div class="m-row-between">
                <h2 class="m-h4">{{ 'Citer cette publication' | t }}</h2>
                <div class="m-seg" role="tablist" [attr.aria-label]="'Format de citation' | t">
                  @for (f of formats; track f.id) {
                    <button type="button" role="tab" [attr.aria-selected]="format() === f.id" [class.is-on]="format() === f.id" (click)="format.set(f.id)">{{ f.label }}</button>
                  }
                </div>
              </div>
              <pre class="m-code">{{ citation() }}</pre>
              <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="copy()">{{ 'Copier' | t }}</button>
            </section>

            <p class="m-ai-note m-small">
              {{ 'Métadonnées proposées par une IA puis vérifiées par un bibliothécaire de' | t }} {{ p.institution }}.
              <a routerLink="/legal" fragment="ia">{{ 'Comment nous utilisons l\\'IA' | t }}</a>
            </p>
          </article>
        </div>
      } @else if (error()) {
        <div class="m-empty">
          <h1 class="m-h2">{{ 'Publication introuvable' | t }}</h1>
          <p>{{ 'Elle n\\'existe pas ou n\\'est pas publique.' | t }}</p>
          <a class="m-btn m-btn--ghost" routerLink="/catalogue">{{ 'Retour au catalogue' | t }}</a>
        </div>
      } @else {
        <p class="m-muted">{{ 'Chargement…' | t }}</p>
      }
    </div>
  `
})
export class FichePage implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);
  readonly typeLabel = typeLabel;
  readonly languageLabel = languageLabel;
  readonly formats: { id: CitationFormat; label: string }[] = [{ id: 'apa', label: 'APA' }, { id: 'bibtex', label: 'BibTeX' }, { id: 'ris', label: 'RIS' }];

  readonly pub = signal<Publication | null>(null);
	readonly translation = signal<PublicationTranslation | null>(null);
  readonly error = signal(false);
  readonly coverUrl = signal<string | null>(null);
  readonly readerUrl = signal<SafeResourceUrl | null>(null);
  readonly format = signal<CitationFormat>('apa');
  private objectUrls: string[] = [];

  readonly citation = computed(() => {
    const p = this.pub();
    if (!p) return '';
    const authors = p.author || '[Auteur inconnu]';
    const year = p.year || 's.d.';
    switch (this.format()) {
      case 'bibtex':
        return `@misc{metamind${p.id},\n  title = {${p.title}},\n  author = {${authors.replace(/, /g, ' and ')}},\n  year = {${year}},\n  institution = {${p.institution}}\n}`;
      case 'ris':
        return ['TY  - GEN', `TI  - ${p.title}`, ...authors.split(/,\s*/).map((a) => `AU  - ${a}`), `PY  - ${year}`, `PB  - ${p.institution}`, 'ER  -'].join('\n');
      default:
        return `${authors} (${year}). ${p.title}. ${p.institution}.`;
    }
  });

	readonly display = computed(() => {
		const p = this.pub();
		const translated = this.translation();
		return {
			title: translated?.title ?? p?.title ?? '',
			summary: translated?.summary ?? p?.summary ?? null,
			keywords: translated?.keywords ?? p?.keywords ?? []
		};
	});

  constructor() {
		effect(() => {
			const p = this.pub();
			const language = this.i18n.lang();
			if (!p) return;
			this.translation.set(null);
			this.api.getPublicationTranslation(p.id, language).subscribe({
				next: (translation) => this.translation.set(translation),
				error: () => undefined
			});
		});
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getPublication(id).subscribe({
      next: (p) => {
        this.pub.set(p);
        if (p.imageUrl) {
          this.api.loadCoverImage(p.id).subscribe({ next: (blob) => this.coverUrl.set(this.track(blob)), error: () => undefined });
        }
      },
      error: () => this.error.set(true)
    });
  }

  displayDate(p: Publication): string {
    // Précision réelle : une date « AAAA-01-01 » issue de l'ancien format est affichée comme l'année seule.
    const d = p.publicationDate ?? '';
    return !d || d.endsWith('-01-01') ? String(p.year) : d;
  }

  openReader(): void {
    const p = this.pub();
    if (!p) return;
    this.api.downloadPublicationFile(p.id).subscribe({
      next: (blob) => this.readerUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.track(blob))),
      error: () => this.toasts.show(this.i18n.t('Le fichier n\'est pas disponible.'), 'error')
    });
  }

  closeReader(): void {
    this.readerUrl.set(null);
  }

  download(): void {
    const p = this.pub();
    if (!p) return;
    this.api.downloadPublicationFile(p.id).subscribe({
      next: (blob) => {
        const link = document.createElement('a');
        link.href = this.track(blob);
        link.download = `${p.title.slice(0, 80)}.pdf`;
        link.click();
      },
      error: () => this.toasts.show(this.i18n.t('Le fichier n\'est pas disponible.'), 'error')
    });
  }

  copy(): void {
    navigator.clipboard?.writeText(this.citation()).then(() => this.toasts.show(this.i18n.t('Citation copiée.')));
  }

  private track(blob: Blob): string {
    const url = URL.createObjectURL(blob);
    this.objectUrls.push(url);
    return url;
  }

  ngOnDestroy(): void {
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
  }
}

import { Component, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiService, Publication, PublicationTranslation } from '../api.service';
import { I18nService, Language, TranslatePipe } from '../core/i18n';
import { classificationLabel, languageLabel, typeLabel } from '../core/labels';
import { TypeCoverComponent } from '../ui/type-cover.component';
import { ToastService } from '../core/toast.service';

const TRANSLATION_RETRY_DELAYS = [5000, 15000, 30000, 60000];

/** Fiche publication (C2) : notice complète, lecture du PDF en ligne, téléchargement. */
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
			  <p class="m-muted m-small">{{ 'Traduction automatique de la notice.' | t }}</p>
			} @else if (translationPending()) {
			  <p class="m-muted m-small">{{ 'Traduction en préparation, la notice s\\'affiche dans sa langue d\\'origine.' | t }}</p>
			}

            <dl class="m-notice">
              @if (p.publicationDate || p.year) { <dt>{{ 'Date' | t }}</dt><dd>{{ displayDate(p) }}</dd> }
              @if (p.documentType) { <dt>{{ 'Type' | t }}</dt><dd>{{ typeLabel(p.documentType) | t }}</dd> }
              @if (p.language) { <dt>{{ 'Langue' | t }}</dt><dd>{{ languageLabel(p.language) | t }}</dd> }
              <dt>{{ 'Institution' | t }}</dt><dd><a routerLink="/catalogue" [queryParams]="{ institution: p.institution }">{{ p.institution }}</a></dd>
              @if (display().classification) { <dt>{{ 'Classification' | t }}</dt><dd>{{ display().classification }}</dd> }
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

            @if (readerOpen()) {
              <section class="m-reader" id="lecteur">
                <div class="m-reader__bar">
                  <strong>{{ 'Lecture en ligne' | t }}</strong>
                  <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="closeReader()">{{ 'Fermer' | t }}</button>
                </div>
                @if (readerUrl(); as url) { <iframe [src]="url" [title]="p.title"></iframe> }
                @else { <p class="m-empty">{{ 'Chargement du document…' | t }}</p> }
              </section>
            }


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

  readonly pub = signal<Publication | null>(null);
	readonly translation = signal<PublicationTranslation | null>(null);
  readonly error = signal(false);
  readonly coverUrl = signal<string | null>(null);
  readonly readerUrl = signal<SafeResourceUrl | null>(null);
  readonly readerOpen = signal(false);
  readonly translationPending = signal(false);
  private translationTimer: ReturnType<typeof setTimeout> | undefined;
  private objectUrls: string[] = [];


	readonly display = computed(() => {
		const p = this.pub();
		const translated = this.translation();
		return {
			title: translated?.title ?? p?.title ?? '',
			summary: translated?.summary ?? p?.summary ?? null,
			keywords: translated?.keywords ?? p?.keywords ?? [],
			classification: classificationLabel(translated?.classification ?? p?.classification, this.i18n.lang()) || null
		};
	});

  constructor() {
		effect(() => {
			const p = this.pub();
			const language = this.i18n.lang();
			if (!p) return;
			this.translation.set(null);
			this.loadTranslation(p.id, language, 0);
		}, { allowSignalWrites: true });
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

  /**
   * La fiche s'affiche tout de suite dans sa langue d'origine. Si la traduction est encore
   * en préparation, on la redemande quelques fois puis on l'affiche dès qu'elle est prête.
   */
  private loadTranslation(id: number, language: Language, attempt: number): void {
    clearTimeout(this.translationTimer);
    this.translationPending.set(false);
    this.api.getPublicationTranslation(id, language).subscribe({
      next: ({ translation, pending }) => {
        if (this.i18n.lang() !== language) return;
        if (!pending) { this.translation.set(translation); return; }
        if (attempt >= TRANSLATION_RETRY_DELAYS.length) return;
        this.translationPending.set(true);
        this.translationTimer = setTimeout(() => this.loadTranslation(id, language, attempt + 1), TRANSLATION_RETRY_DELAYS[attempt]);
      },
      error: () => undefined
    });
  }

  displayDate(p: Publication): string {
    // Précision réelle : une date « AAAA-01-01 » issue de l'ancien format est affichée comme l'année seule.
    const d = p.publicationDate ?? '';
    return !d || d.endsWith('-01-01') ? String(p.year) : d;
  }

  /**
   * Le lecteur s'ouvre et la page y descend tout de suite ; le PDF s'affiche dès qu'il est
   * téléchargé. Attendre le fichier avant de réagir donnait l'impression d'un bouton inactif.
   */
  openReader(): void {
    const p = this.pub();
    if (!p) return;
    this.readerOpen.set(true);
    this.scrollToReader(0);
    if (this.readerUrl()) return;
    this.api.downloadPublicationFile(p.id).subscribe({
      next: (blob) => this.readerUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.track(blob))),
      error: () => {
        this.readerOpen.set(false);
        this.toasts.show(this.i18n.t('Le fichier n\'est pas disponible.'), 'error');
      }
    });
  }

  /** Le lecteur n'existe qu'après le prochain rendu : on le cherche quelques fois avant de défiler. */
  private scrollToReader(attempt: number): void {
    const reader = document.getElementById('lecteur');
    if (!reader) {
      if (attempt < 20) setTimeout(() => this.scrollToReader(attempt + 1), 50);
      return;
    }
    const top = reader.getBoundingClientRect().top + window.scrollY - 88;
    window.scrollTo({ top, behavior: 'smooth' });
  }

  closeReader(): void {
    this.readerOpen.set(false);
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


  private track(blob: Blob): string {
    const url = URL.createObjectURL(blob);
    this.objectUrls.push(url);
    return url;
  }

  ngOnDestroy(): void {
    clearTimeout(this.translationTimer);
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
  }
}

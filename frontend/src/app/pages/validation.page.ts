import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, MetadataAuthor, MetadataDetails, Publication } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { DOCUMENT_TYPES, REFERENCE_LANGUAGES, confidenceLevel, languageLabel, typeLabel } from '../core/labels';
import { ToastService } from '../core/toast.service';
import { ConfidenceComponent } from '../ui/confidence.component';
import { TypeCoverComponent } from '../ui/type-cover.component';

type FieldId = 'titre' | 'auteurs' | 'resume' | 'mots_cles' | 'date' | 'classification';
interface FieldDef { id: FieldId; label: string; dc: string; required: boolean; }

/**
 * Validation des métadonnées (B6) : document à gauche, notice à droite.
 * Les scores de confiance s'affichent dès que l'API les fournit (prompt 2) ; en attendant « Confiance non calculée ».
 */
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe, ConfidenceComponent, TypeCoverComponent],
  template: `
    @if (meta(); as m) {
      <div class="m-vhead">
        <div class="m-wrap m-vhead__in">
          <div class="m-vhead__title">
            <nav class="m-small m-muted"><a routerLink="/espace/file">{{ 'File de validation' | t }}</a>
              @if (position()) { · {{ 'Document' | t }} {{ position() }} {{ 'sur' | t }} {{ queue().length }} }
            </nav>
            <h1 class="m-title">{{ titre || ('Sans titre' | t) }}</h1>
            <p class="m-muted m-small">
              <span class="m-badge m-badge--A_VALIDER">{{ 'À valider' | t }}</span>
              @if (pub()?.documentType) { · {{ typeLabel(pub()?.documentType) | t }} }
              @if (pub()?.language) { · {{ languageLabel(pub()?.language) | t }} }
            </p>
          </div>
          <div class="m-actions">
            @if (queue().length > 1) { <button type="button" class="m-btn m-btn--ghost" [disabled]="busy()" (click)="skip()">{{ 'Passer' | t }}</button> }
            <button type="button" class="m-btn m-btn--danger" (click)="rejectOpen.set(true)">{{ 'Rejeter…' | t }}</button>
            <button type="button" class="m-btn m-btn--primary" [disabled]="!canPublish() || busy()" (click)="publish()">
              {{ missing().length ? ('Publier' | t) + ' (' + missing().length + ' ' + ('à compléter' | t) + ')' : ('Publier' | t) }}
            </button>
          </div>
          <div class="m-rail" role="group" [attr.aria-label]="'Avancement de la vérification' | t">
            @for (f of fields; track f.id) {
              <button type="button" (click)="select(f.id)" [class.is-sel]="selected() === f.id" [attr.aria-label]="(f.label | t) + ' : ' + (stateLabel(f.id) | t)">
                <span [class]="'m-rail__bar is-' + stateOf(f.id)"></span>
                <span class="m-rail__label">{{ f.label | t }}</span>
              </button>
            }
          </div>
        </div>
      </div>

      @if (rejectOpen()) {
        <div class="m-wrap">
          <form class="m-sheet m-sheet--pad m-stack m-reject" (ngSubmit)="reject()">
            <label class="m-field">{{ 'Motif du rejet' | t }} <span class="m-muted m-small">({{ 'obligatoire, visible dans l\\'historique' | t }})</span>
              <textarea class="m-input" rows="2" name="motif" [(ngModel)]="reason" required></textarea>
            </label>
            <div class="m-actions m-actions--end">
              <button type="button" class="m-btn m-btn--ghost" (click)="rejectOpen.set(false)">{{ 'Annuler' | t }}</button>
              <button type="submit" class="m-btn m-btn--danger-solid" [disabled]="reason.trim().length < 10 || busy()">{{ 'Rejeter le document' | t }}</button>
            </div>
          </form>
        </div>
      }

      <div class="m-wrap m-vbody">
        <section class="m-viewer" [attr.aria-label]="'Document' | t">
          <div class="m-viewer__bar">
            <div class="m-seg">
              <button type="button" [class.is-on]="view() === 'pdf'" (click)="showPdf()">{{ 'Document' | t }}</button>
              <button type="button" [class.is-on]="view() === 'texte'" (click)="view.set('texte')">{{ 'Texte extrait' | t }}</button>
            </div>
          </div>
          @if (view() === 'pdf') {
            @if (pdfUrl()) { <iframe [src]="pdfUrl()" [title]="'Document' | t"></iframe> }
            @else { <p class="m-empty">{{ 'Chargement du document…' | t }}</p> }
          } @else {
            <pre class="m-viewer__text">{{ m.texte_extrait || ('Aucun texte extrait.' | t) }}</pre>
          }
        </section>

        <section class="m-notice-sheet">
          <div class="m-notice-sheet__head">
            <div>
              <h2 class="m-h3">{{ 'Notice' | t }}</h2>
              <p class="m-muted m-small">{{ 'Relisez chaque champ, corrigez si besoin, puis confirmez. Rien n\\'est publié sans votre validation.' | t }}</p>
            </div>
          </div>

          @for (f of fields; track f.id) {
            <article class="m-nrow" [class.is-sel]="selected() === f.id" (click)="select(f.id)">
              <div class="m-nrow__label">
                <strong>{{ f.label | t }}@if (f.required) { <span aria-hidden="true"> *</span> }</strong>
                <code>{{ f.dc }}</code>
                <m-confidence [score]="score(f.id)" [confirmed]="confirmed().has(f.id)" [empty]="isEmpty(f.id)" [required]="f.required" />
              </div>
              <div class="m-nrow__body">
                @switch (f.id) {
                  @case ('titre') { <input class="m-input" name="titre" [(ngModel)]="titre" (ngModelChange)="touch('titre')" [attr.aria-label]="f.label | t" /> }
                  @case ('resume') { <textarea class="m-input" rows="5" name="resume" [(ngModel)]="resume" (ngModelChange)="touch('resume')" [attr.aria-label]="f.label | t"></textarea> }
                  @case ('date') {
                    <input class="m-input m-input--short" name="date" [(ngModel)]="date" (ngModelChange)="touch('date')" placeholder="AAAA ou AAAA-MM-JJ" [attr.aria-label]="f.label | t" />
                    <span class="m-muted m-small">{{ 'Gardez la précision du document : l\\'année seule si le jour n\\'est pas indiqué.' | t }}</span>
                  }
                  @case ('classification') { <input class="m-input" name="classification" [(ngModel)]="classification" (ngModelChange)="touch('classification')" [attr.aria-label]="f.label | t" /> }
                  @case ('mots_cles') {
                    <div class="m-chipbox">
                      @for (k of keywords(); track k) {
                        <span class="m-chip">{{ k }}<button type="button" (click)="removeKeyword(k)" [attr.aria-label]="('Retirer' | t) + ' ' + k">×</button></span>
                      }
                      <input class="m-chipbox__input" name="nouveauMot" [(ngModel)]="newKeyword" (keydown.enter)="$event.preventDefault(); addKeyword()" [placeholder]="'Ajouter un mot-clé…' | t" />
                    </div>
                  }
                  @case ('auteurs') {
                    @if (!authors().length) { <p class="m-hint">{{ 'Aucun auteur proposé. Ajoutez-les depuis la page de titre du document.' | t }}</p> }
                    @for (a of authors(); track $index; let i = $index) {
                      <div class="m-author">
                        <input class="m-input" [name]="'nom' + i" [ngModel]="a.nom_complet" (ngModelChange)="setAuthor(i, 'nom_complet', $event)" [placeholder]="'Nom, Prénom' | t" [attr.aria-label]="'Nom de l\\'auteur' | t" />
                        <input class="m-input m-mono" [name]="'orcid' + i" [ngModel]="a.orcid ?? ''" (ngModelChange)="setAuthor(i, 'orcid', $event)" placeholder="ORCID" [attr.aria-label]="'ORCID' | t" />
                        <button type="button" class="m-btn m-btn--ghost m-btn--icon" (click)="removeAuthor(i)" [attr.aria-label]="'Retirer l\\'auteur' | t">×</button>
                      </div>
                    }
                    <button type="button" class="m-btn m-btn--ghost m-btn--sm" (click)="addAuthor()">+ {{ 'Ajouter un auteur' | t }}</button>
                  }
                }
                <div class="m-nrow__foot">
                  <span class="m-muted m-small">{{ 'Proposé par l\\'IA à partir du texte extrait.' | t }}</span>
                  <button type="button" class="m-btn m-btn--sm" [class.m-btn--confirmed]="confirmed().has(f.id)" [class.m-btn--ghost]="!confirmed().has(f.id)" [disabled]="isEmpty(f.id)" (click)="$event.stopPropagation(); toggleConfirm(f.id)">
                    {{ (confirmed().has(f.id) ? 'Vérifié' : 'Confirmer') | t }}
                  </button>
                </div>
              </div>
            </article>
          }

          <div class="m-notice-sheet__foot">
            <div><strong>{{ 'Langue, type et DOI' | t }}</strong>
              <p class="m-muted m-small">{{ 'Détectés automatiquement dans le texte du document. Corrigez-les si besoin.' | t }}</p></div>
            <div class="m-actions m-actions--row">
              <label class="m-field">{{ 'Langue' | t }}
                <select class="m-input m-input--sm" name="langue" [(ngModel)]="langue">
                  <option value="">{{ 'Non précisée' | t }}</option>
                  @for (code of languages; track code) { <option [value]="code">{{ languageLabel(code) | t }}</option> }
                </select>
              </label>
              <label class="m-field">{{ 'Type' | t }}
                <select class="m-input m-input--sm" name="typeDocument" [(ngModel)]="typeDocument">
                  <option value="">{{ 'Non précisé' | t }}</option>
                  @for (code of documentTypes; track code) { <option [value]="code">{{ typeLabel(code) | t }}</option> }
                </select>
              </label>
              <label class="m-field">DOI
                <input class="m-input m-input--sm m-mono" name="doi" [(ngModel)]="doi" />
              </label>
            </div>
          </div>
          <div class="m-notice-sheet__foot">
            <div><strong>{{ 'Image de la notice' | t }}</strong>
              <p class="m-muted m-small">{{ 'Affichée dans le catalogue. Par défaut, la première page du document.' | t }}</p></div>
            <div class="m-actions">
              <m-type-cover [type]="pub()?.documentType" [imageUrl]="pub()?.imageUrl" />
              <label class="m-btn m-btn--ghost m-btn--sm">{{ 'Changer l\\'image' | t }}
                <input class="m-sr" type="file" accept="image/jpeg,image/png,image/webp" (change)="changeImage($event)" />
              </label>
            </div>
          </div>
          <div class="m-notice-sheet__foot">
            <div><strong>{{ 'Visibilité après publication' | t }}</strong>
              <p class="m-muted m-small">{{ 'Public : visible par tous. Institution : réservé aux bibliothécaires de votre institution.' | t }}</p></div>
            <div class="m-seg" role="radiogroup" [attr.aria-label]="'Visibilité' | t">
              <button type="button" role="radio" [attr.aria-checked]="visibility === 'PUBLIC'" [class.is-on]="visibility === 'PUBLIC'" (click)="visibility = 'PUBLIC'">{{ 'Public' | t }}</button>
              <button type="button" role="radio" [attr.aria-checked]="visibility === 'INSTITUTION'" [class.is-on]="visibility === 'INSTITUTION'" (click)="visibility = 'INSTITUTION'">{{ 'Institution' | t }}</button>
            </div>
          </div>
        </section>
      </div>
    } @else if (failed()) {
      <div class="m-wrap m-page m-empty">
        <p>{{ 'Les métadonnées de ce document ne sont pas encore disponibles. Lancez l\\'extraction depuis la file.' | t }}</p>
        <a class="m-btn m-btn--ghost" routerLink="/espace/file">{{ 'Retour à la file' | t }}</a>
      </div>
    } @else {
      <p class="m-wrap m-page m-muted">{{ 'Chargement…' | t }}</p>
    }
  `
})
export class ValidationPage implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);
  readonly typeLabel = typeLabel;
  readonly languageLabel = languageLabel;
  readonly languages = REFERENCE_LANGUAGES;
  readonly documentTypes = Object.keys(DOCUMENT_TYPES);
  readonly fields: FieldDef[] = [
    { id: 'titre', label: 'Titre', dc: 'dc:title', required: true },
    { id: 'auteurs', label: 'Auteurs', dc: 'dc:creator', required: true },
    { id: 'resume', label: 'Résumé', dc: 'dc:description', required: false },
    { id: 'mots_cles', label: 'Mots-clés', dc: 'dc:subject', required: false },
    { id: 'date', label: 'Date', dc: 'dc:date', required: true },
    { id: 'classification', label: 'Classification', dc: 'dc:subject', required: false }
  ];

  /**
   * Document affiché. Angular réutilise la page quand seul l'identifiant change dans l'URL
   * (« Publier », « Passer ») : l'identifiant est donc suivi, pas lu une seule fois.
   */
  private id = 0;
  readonly meta = signal<MetadataDetails | null>(null);
  readonly pub = signal<Publication | null>(null);
  readonly failed = signal(false);
  readonly busy = signal(false);
  readonly selected = signal<FieldId>('titre');
  readonly confirmed = signal(new Set<FieldId>());
  readonly view = signal<'pdf' | 'texte'>('pdf');
  readonly pdfUrl = signal<SafeResourceUrl | null>(null);
  readonly rejectOpen = signal(false);
  readonly authors = signal<MetadataAuthor[]>([]);
  readonly keywords = signal<string[]>([]);
  /** Notices à valider, dans l'ordre de la file : position affichée et document suivant. */
  readonly queue = signal<Publication[]>([]);
  readonly position = computed(() => {
    const index = this.queue().findIndex((d) => d.id === this.id);
    return index < 0 ? 0 : index + 1;
  });
  private objectUrl = '';

  titre = '';
  resume = '';
  date = '';
  classification = '';
  langue = '';
  typeDocument = '';
  doi = '';
  newKeyword = '';
  reason = '';
  visibility: 'PUBLIC' | 'INSTITUTION' = 'PUBLIC';

  missing(): FieldDef[] {
    return this.fields.filter((f) => f.required && this.isEmpty(f.id));
  }

  canPublish(): boolean {
    return this.missing().length === 0;
  }

  constructor() {
    this.route.paramMap.subscribe((params) => this.load(Number(params.get('id'))));
  }

  private load(id: number): void {
    this.id = id;
    this.releasePdf();
    this.meta.set(null);
    this.pub.set(null);
    this.failed.set(false);
    this.busy.set(false);
    this.rejectOpen.set(false);
    this.reason = '';
    this.selected.set('titre');
    this.confirmed.set(new Set<FieldId>());
    this.api.getManagedDocuments('A_VALIDER').subscribe({ next: (docs) => this.queue.set(docs), error: () => undefined });
    this.api.getPublication(id).subscribe({ next: (p) => this.pub.set(p), error: () => undefined });
    this.api.getMetadata(id).subscribe({
      next: (m) => {
        this.meta.set(m);
        this.titre = m.titre ?? '';
        this.resume = m.resume ?? '';
        this.date = this.cleanDate(m.date_publication);
        this.classification = m.classification ?? '';
        this.langue = m.langue ?? '';
        this.typeDocument = m.type_document ?? '';
        this.doi = m.doi ?? '';
        this.visibility = m.visibilite ?? 'PUBLIC';
        this.authors.set((m.auteurs ?? []).map((a) => ({ ...a })));
        this.keywords.set([...(m.mots_cles ?? [])]);
        this.showPdf();
      },
      error: () => this.failed.set(true)
    });
  }

  select(id: FieldId): void { this.selected.set(id); }

  isEmpty(id: FieldId): boolean {
    switch (id) {
      case 'titre': return !this.titre.trim();
      case 'auteurs': return !this.authors().some((a) => a.nom_complet.trim());
      case 'resume': return !this.resume.trim();
      case 'mots_cles': return this.keywords().length === 0;
      case 'date': return !this.date.trim();
      case 'classification': return !this.classification.trim();
    }
  }

  /** Score de confiance calculé à l'extraction ; les clés sont celles de l'API. */
  score(id: FieldId): number | null {
    const key = id === 'date' ? 'date_publication' : id;
    return this.meta()?.confiances?.[key] ?? null;
  }

  stateOf(id: FieldId): string {
    if (this.isEmpty(id)) return 'low';
    if (this.confirmed().has(id)) return 'done';
    const level = confidenceLevel(this.score(id));
    return level === 'unknown' ? 'unknown' : level;
  }

  stateLabel(id: FieldId): string {
    if (this.isEmpty(id)) return this.fields.find((f) => f.id === id)?.required ? 'à compléter' : 'vide';
    return { done: 'vérifié', high: 'à relire', mid: 'à relire', low: 'à corriger', unknown: 'à relire' }[this.stateOf(id)] ?? '';
  }

  /** Corriger un champ vaut vérification, sauf s'il a été vidé : un champ vide n'est jamais vérifié. */
  touch(id: FieldId): void {
    this.confirmed.update((s) => {
      const n = new Set(s);
      this.isEmpty(id) ? n.delete(id) : n.add(id);
      return n;
    });
  }

  toggleConfirm(id: FieldId): void {
    if (this.isEmpty(id)) return;
    this.confirmed.update((s) => { const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n; });
  }

  setAuthor(i: number, key: 'nom_complet' | 'orcid', value: string): void {
    this.authors.update((list) => list.map((a, j) => (j === i ? { ...a, [key]: value } : a)));
    this.touch('auteurs');
  }
  addAuthor(): void { this.authors.update((l) => [...l, { nom_complet: '' }]); }
  removeAuthor(i: number): void { this.authors.update((l) => l.filter((_, j) => j !== i)); this.touch('auteurs'); }
  addKeyword(): void {
    const k = this.newKeyword.trim();
    if (k && !this.keywords().includes(k)) { this.keywords.update((l) => [...l, k]); this.touch('mots_cles'); }
    this.newKeyword = '';
  }
  removeKeyword(k: string): void { this.keywords.update((l) => l.filter((x) => x !== k)); this.touch('mots_cles'); }

  showPdf(): void {
    this.view.set('pdf');
    if (this.pdfUrl()) return;
    this.api.downloadPublicationFile(this.id).subscribe({
      next: (blob) => { this.objectUrl = URL.createObjectURL(blob); this.pdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl)); },
      error: () => this.view.set('texte')
    });
  }

  publish(): void {
    if (!this.canPublish()) return;
    this.busy.set(true);
    this.api.validateMetadata(this.id, {
      titre: this.titre.trim(),
      resume: this.resume.trim(),
      date_publication: this.toApiDate(this.date),
      classification: this.classification.trim(),
      visibilite: this.visibility,
      auteurs: this.authors().filter((a) => a.nom_complet.trim()).map((a) => ({ nom_complet: a.nom_complet.trim(), ...(a.orcid?.trim() ? { orcid: a.orcid.trim() } : {}) })),
      mots_cles: this.keywords(),
      langue: this.langue || null,
      type_document: this.typeDocument || null,
      doi: this.doi.trim() || null
    }).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Publié. La fiche est dans le catalogue.')); this.next(); },
      error: () => { this.busy.set(false); this.toasts.show(this.i18n.t('La publication a échoué. Vérifiez les champs.'), 'error'); }
    });
  }

  changeImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.api.replaceDocumentImage(this.id, file).subscribe({
      next: (p) => { this.pub.set(p); this.toasts.show(this.i18n.t('Image mise à jour.')); },
      error: (e) => this.toasts.show(e?.error?.message ?? this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  reject(): void {
    this.busy.set(true);
    this.api.rejectMetadata(this.id, this.reason.trim()).subscribe({
      next: () => { this.toasts.show(this.i18n.t('Document rejeté.')); this.next(); },
      error: () => { this.busy.set(false); this.toasts.show(this.i18n.t('Le rejet a échoué.'), 'error'); }
    });
  }

  /** Laisse ce document dans la file et ouvre le suivant. */
  skip(): void {
    this.open(this.following(true));
  }

  /** Après publication ou rejet : le document a quitté la file, on ouvre le suivant ou on revient à la file. */
  private next(): void {
    this.open(this.following(false));
  }

  private following(wrap: boolean): Publication | undefined {
    const list = this.queue();
    const index = list.findIndex((d) => d.id === this.id);
    const after = list.slice(index + 1);
    const before = wrap ? list.slice(0, Math.max(index, 0)) : list.filter((d) => d.id !== this.id).slice(0, Math.max(index, 0));
    return [...after, ...before].find((d) => d.id !== this.id);
  }

  private open(doc: Publication | undefined): void {
    this.router.navigateByUrl(doc ? `/espace/documents/${doc.id}/validation` : '/espace/file');
  }

  private cleanDate(value: string | null): string {
    if (!value) return '';
    return value.endsWith('-01-01') ? value.slice(0, 4) : value;
  }

  /** L'API actuelle attend une date complète : une année seule est envoyée au 1er janvier (à corriger côté backend, prompt 2). */
  private toApiDate(value: string): string | null {
    const v = value.trim();
    if (/^\d{4}$/.test(v)) return `${v}-01-01`;
    if (/^\d{4}-\d{2}$/.test(v)) return `${v}-01`;
    return v || null;
  }

  ngOnDestroy(): void {
    this.releasePdf();
  }

  private releasePdf(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl = '';
    this.pdfUrl.set(null);
    this.view.set('pdf');
  }
}

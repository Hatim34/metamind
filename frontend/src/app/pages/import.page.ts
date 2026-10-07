import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../api.service';
import { TranslatePipe } from '../core/i18n';

/**
 * Étapes suivies en direct pour chaque fichier :
 * prêt -> envoi -> lecture du fichier -> analyse par l'IA -> à valider,
 * ou non extrait (import seul, crédits insuffisants, analyse en échec), ou erreur.
 */
type Step = 'pret' | 'envoi' | 'lecture' | 'analyse' | 'a_valider' | 'non_extrait' | 'erreur';
interface Item { file: File; state: Step; message: string; id?: number; }

/** Import (B3) : le parcours complet reste sur cette page jusqu'à ce que les notices soient prêtes. */
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe],
  template: `
    <div class="m-wrap m-wrap--narrow m-page">
      <div class="m-page__head">
        <div>
          <h1 class="m-title">{{ 'Importer des documents' | t }}</h1>
          <p class="m-muted">{{ 'PDF, DOCX ou TXT. L\\'extraction coûte 1 crédit par document, rendu en cas d\\'échec.' | t }}</p>
        </div>
      </div>

      @if (!busy()) {
        <label class="m-drop" [class.is-over]="over()" (dragover)="$event.preventDefault(); over.set(true)" (dragleave)="over.set(false)" (drop)="onDrop($event)">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" aria-hidden="true"><path d="M12 15V4M7.5 8.5 12 4l4.5 4.5"/><path d="M4 15v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4"/></svg>
          <strong>{{ 'Déposez vos fichiers ici' | t }}</strong>
          <span class="m-muted">{{ 'ou cliquez pour parcourir votre ordinateur' | t }}</span>
          <input class="m-sr" type="file" multiple accept=".pdf,.docx,.txt" (change)="onPick($event)" />
        </label>
      }

      @if (items().length) {
        <section class="m-sheet">
          <div class="m-sheet__head"><h2 class="m-h4">{{ items().length }} {{ 'fichiers' | t }}</h2></div>
          @for (item of items(); track item.file.name + item.file.size) {
            <div class="m-line">
              <span class="m-line__main">
                <span class="m-row-between"><strong class="m-ellipsis">{{ item.file.name }}</strong><span class="m-muted m-small">{{ size(item.file.size) }}</span></span>
                <span class="m-progress"><span [class]="'m-progress__fill is-' + item.state"></span></span>
              </span>
              <span [class]="'m-state m-state--' + item.state">{{ item.message | t }}</span>
            </div>
          }

          @if (pending().length) {
            <div class="m-sheet__foot">
              <label class="m-inline m-small">{{ 'Visibilité par défaut' | t }}
                <select class="m-input m-input--sm" [(ngModel)]="visibility" name="visibilite">
                  <option value="PUBLIC">{{ 'Public' | t }}</option>
                  <option value="INSTITUTION">{{ 'Institution' | t }}</option>
                </select>
              </label>
              <div class="m-actions">
                <button type="button" class="m-btn m-btn--ghost" [disabled]="busy()" (click)="upload(false)">{{ 'Importer sans extraire' | t }}</button>
                <button type="button" class="m-btn m-btn--primary" [disabled]="busy()" (click)="upload(true)">
                  {{ 'Importer et lancer l\\'extraction' | t }} ({{ pending().length }} {{ 'crédits' | t }})
                </button>
              </div>
            </div>
          }

          @if (finished()) {
            <div class="m-import-done">
              <span>
                @if (ready().length) { <strong>{{ ready().length }} {{ 'notices prêtes à valider' | t }}</strong> }
                @if (waiting().length) { <span class="m-muted"> · {{ waiting().length }} {{ 'en attente d\\'extraction' | t }}</span> }
                @if (failed().length) { <span class="m-muted"> · {{ failed().length }} {{ 'en erreur' | t }}</span> }
              </span>
              <span class="m-actions">
                <button type="button" class="m-btn m-btn--ghost" (click)="reset()">{{ 'Importer d\\'autres fichiers' | t }}</button>
                @if (waiting().length) { <a class="m-btn m-btn--ghost" routerLink="/espace/file" [queryParams]="{ statut: 'EN_ATTENTE' }">{{ 'Voir les non extraits' | t }}</a> }
                @if (ready().length) { <button type="button" class="m-btn m-btn--primary" (click)="validate()">{{ 'Commencer la validation' | t }}</button> }
              </span>
            </div>
          }
        </section>
      }
    </div>
  `
})
export class ImportPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private static readonly maxBytes = 50 * 1024 * 1024;
  private static readonly allowed = ['pdf', 'docx', 'txt'];

  visibility: 'PUBLIC' | 'INSTITUTION' = 'PUBLIC';
  readonly over = signal(false);
  readonly busy = signal(false);
  readonly finished = signal(false);
  readonly items = signal<Item[]>([]);
  readonly pending = computed(() => this.items().filter((i) => i.state === 'pret'));
  readonly ready = computed(() => this.items().filter((i) => i.state === 'a_valider'));
  readonly waiting = computed(() => this.items().filter((i) => i.state === 'non_extrait'));
  readonly failed = computed(() => this.items().filter((i) => i.state === 'erreur'));

  onPick(event: Event): void {
    this.add(Array.from((event.target as HTMLInputElement).files ?? []));
    (event.target as HTMLInputElement).value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.over.set(false);
    this.add(Array.from(event.dataTransfer?.files ?? []));
  }

  size(bytes: number): string {
    return bytes > 1048576 ? (bytes / 1048576).toFixed(1).replace('.', ',') + ' Mo' : Math.ceil(bytes / 1024) + ' Ko';
  }

  reset(): void {
    this.items.set([]);
    this.finished.set(false);
  }

  /** Ouvre la première notice prête ; la page de validation enchaîne ensuite sur les suivantes. */
  validate(): void {
    const first = this.ready()[0];
    if (first?.id) this.router.navigate(['/espace/documents', first.id, 'validation']);
  }

  private add(files: File[]): void {
    if (this.finished()) this.reset();
    const next = files.map<Item>((file) => {
      const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
      if (!ImportPage.allowed.includes(ext)) return { file, state: 'erreur', message: 'Format non pris en charge' };
      if (file.size > ImportPage.maxBytes) return { file, state: 'erreur', message: 'Fichier trop volumineux (50 Mo max.)' };
      return { file, state: 'pret', message: 'Prêt' };
    });
    this.items.update((list) => [...list, ...next]);
  }

  /** Envoie tous les fichiers, puis suit chacun jusqu'à sa notice : aucune redirection pendant le traitement. */
  async upload(extract: boolean): Promise<void> {
    this.busy.set(true);
    for (const item of [...this.pending()]) {
      this.patch(item, { state: 'envoi', message: 'Envoi…' });
      try {
        const doc = await firstValueFrom(this.api.importDocument(item.file, this.visibility));
        this.patch(item, { id: doc.id, state: 'lecture', message: 'Lecture du fichier…' });
      } catch (e: any) {
        // Le serveur précise la cause (taille réglée par l'administrateur, format refusé).
        this.patch(item, { state: 'erreur', message: e?.status === 409 ? 'Doublon possible' : e?.error?.message ?? 'Échec de l\'import' });
      }
    }
    for (const item of this.items().filter((i) => i.state === 'lecture')) {
      await this.process(item, extract);
    }
    this.busy.set(false);
    this.finished.set(true);
  }

  private async process(item: Item, extract: boolean): Promise<void> {
    const doc = await firstValueFrom(this.api.waitForText(item.id!)).catch(() => null);
    if (doc?.status === 'ECHEC') {
      this.patch(item, { state: 'erreur', message: 'Fichier illisible' });
      return;
    }
    if (!extract) {
      this.patch(item, { state: 'non_extrait', message: doc ? 'Texte lu, prêt pour l\'extraction' : 'Lecture du fichier en cours' });
      return;
    }
    this.patch(item, { state: 'analyse', message: 'Analyse par l\'IA…' });
    try {
      await firstValueFrom(this.api.extractMetadata(item.id!));
      this.patch(item, { state: 'a_valider', message: 'Notice prête à valider' });
    } catch (e: any) {
      this.patch(item, { state: 'non_extrait', message: e?.status === 402 ? 'Crédits insuffisants' : 'Analyse impossible, crédit rendu' });
    }
  }

  private patch(item: Item, change: Partial<Item>): void {
    this.items.update((list) => list.map((i) => (i === item ? Object.assign(item, change) : i)));
  }
}

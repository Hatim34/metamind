import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { ToastService } from '../core/toast.service';

interface Item { file: File; state: 'pret' | 'envoi' | 'importe' | 'erreur'; message: string; id?: number; }

/** Import : PDF, DOCX, TXT jusqu'a la limite appliquee par le backend. */
@Component({
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  template: `
    <div class="m-wrap m-wrap--narrow m-page">
      <div class="m-page__head">
        <div>
          <h1 class="m-title">{{ 'Importer des documents' | t }}</h1>
          <p class="m-muted">{{ 'PDF, DOCX ou TXT, jusqu\\'à 50 Mo chacun. L\\'extraction coûte 1 crédit par document, rendu en cas d\\'échec.' | t }}</p>
        </div>
      </div>

      <label class="m-drop" [class.is-over]="over()" (dragover)="$event.preventDefault(); over.set(true)" (dragleave)="over.set(false)" (drop)="onDrop($event)">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" aria-hidden="true"><path d="M12 15V4M7.5 8.5 12 4l4.5 4.5"/><path d="M4 15v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4"/></svg>
        <strong>{{ 'Déposez vos fichiers ici' | t }}</strong>
        <span class="m-muted">{{ 'ou cliquez pour parcourir votre ordinateur' | t }}</span>
        <input class="m-sr" type="file" multiple accept=".pdf,.docx,.txt" (change)="onPick($event)" />
      </label>

      @if (items().length) {
        <section class="m-sheet">
          <div class="m-sheet__head"><h2 class="m-h4">{{ items().length }} {{ 'fichiers' | t }}</h2><span class="m-muted m-small">{{ imported().length }} {{ 'importés' | t }}</span></div>
          @for (item of items(); track item.file.name + item.file.size) {
            <div class="m-line">
              <span class="m-line__main">
                <span class="m-row-between"><strong class="m-ellipsis">{{ item.file.name }}</strong><span class="m-muted m-small">{{ size(item.file.size) }}</span></span>
                <span class="m-progress"><span [class]="'m-progress__fill is-' + item.state"></span></span>
              </span>
              <span [class]="'m-state m-state--' + item.state">{{ item.message | t }}</span>
            </div>
          }
          <div class="m-sheet__foot">
            <label class="m-inline m-small">{{ 'Visibilité par défaut' | t }}
              <select class="m-input m-input--sm" [(ngModel)]="visibility" name="visibilite">
                <option value="PUBLIC">{{ 'Public' | t }}</option>
                <option value="INSTITUTION">{{ 'Institution' | t }}</option>
              </select>
            </label>
            <div class="m-actions">
              <button type="button" class="m-btn m-btn--ghost" [disabled]="busy() || !pending().length" (click)="upload(false)">{{ 'Importer sans extraire' | t }}</button>
              <button type="button" class="m-btn m-btn--primary" [disabled]="busy() || !pending().length" (click)="upload(true)">
                {{ 'Importer et lancer l\\'extraction' | t }} ({{ pending().length }} {{ 'crédits' | t }})
              </button>
            </div>
          </div>
        </section>
      }
    </div>
  `
})
export class ImportPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);
  private static readonly maxBytes = 50 * 1024 * 1024;
  private static readonly allowed = ['pdf', 'docx', 'txt'];

  visibility: 'PUBLIC' | 'INSTITUTION' = 'PUBLIC';
  readonly over = signal(false);
  readonly busy = signal(false);
  readonly items = signal<Item[]>([]);
  readonly pending = computed(() => this.items().filter((i) => i.state === 'pret'));
  readonly imported = computed(() => this.items().filter((i) => i.state === 'importe'));

  onPick(event: Event): void {
    this.add(Array.from((event.target as HTMLInputElement).files ?? []));
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.over.set(false);
    this.add(Array.from(event.dataTransfer?.files ?? []));
  }

  size(bytes: number): string {
    return bytes > 1048576 ? (bytes / 1048576).toFixed(1).replace('.', ',') + ' Mo' : Math.ceil(bytes / 1024) + ' Ko';
  }

  private add(files: File[]): void {
    const next = files.map<Item>((file) => {
      const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
      if (!ImportPage.allowed.includes(ext)) return { file, state: 'erreur', message: 'Format non pris en charge' };
      if (file.size > ImportPage.maxBytes) return { file, state: 'erreur', message: 'Fichier trop volumineux (50 Mo max.)' };
      return { file, state: 'pret', message: 'Prêt' };
    });
    this.items.update((list) => [...list, ...next]);
  }

  upload(extract: boolean): void {
    this.busy.set(true);
    const queue = [...this.pending()];
    const step = (): void => {
      const item = queue.shift();
      if (!item) {
        this.busy.set(false);
        const count = this.imported().length;
        if (count) {
          this.toasts.show(`${count} ${this.i18n.t(extract ? 'documents importés, extraction lancée.' : 'documents importés.')}`);
          this.router.navigate(['/espace/file'], { queryParams: { statut: extract ? 'EXTRACTION' : 'EN_ATTENTE' } });
        }
        return;
      }
      this.patch(item, { state: 'envoi', message: 'Envoi…' });
      this.api.importDocument(item.file, this.visibility).subscribe({
        next: (doc) => {
          this.patch(item, { state: 'importe', message: 'Importé', id: doc.id });
          if (extract) {
            this.api.extractMetadata(doc.id).subscribe({
              error: (e) => this.toasts.show(this.i18n.t(e?.status === 402 ? 'Crédits insuffisants pour lancer l\'extraction.' : 'L\'extraction n\'a pas pu être lancée.'), 'error')
            });
          }
          step();
        },
        error: (e) => {
          this.patch(item, { state: 'erreur', message: e?.status === 409 ? 'Doublon possible' : 'Échec de l\'import' });
          step();
        }
      });
    };
    step();
  }

  private patch(item: Item, change: Partial<Item>): void {
    this.items.update((list) => list.map((i) => (i === item ? Object.assign(item, change) : i)));
  }
}

import { Component, OnDestroy, computed, effect, inject, input, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ApiService } from '../api.service';
import { coverKind, typeLabel } from '../core/labels';
import { TranslatePipe } from '../core/i18n';

/** Couverture typée : type + année dans la couleur du type. Repli quand il n'y a pas de vignette. */
@Component({
  selector: 'm-type-cover',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    @if (coverUrl()) {
      <img [class]="'m-cover m-cover--img m-cover--' + size()" [src]="coverUrl()" alt="" loading="lazy" (error)="markFailed()" />
    } @else {
      <span [class]="'m-cover m-cover--' + size() + ' m-cover--' + kind()" aria-hidden="true">
        <span class="m-cover__type">{{ (size() === 'sm' ? short() : label()) | t }}</span>
        @if (size() !== 'sm' && year()) { <span class="m-cover__year">{{ year() }}</span> }
      </span>
    }
  `
})
export class TypeCoverComponent implements OnDestroy {
  private readonly api = inject(ApiService);
  readonly type = input<string | null | undefined>('');
  readonly year = input<number | string | null | undefined>('');
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly imageUrl = input<string | null | undefined>(null);
  /** Vignette indisponible : on retombe sur la couverture typée. */
  readonly failedUrl = signal<string | null>(null);
  readonly coverUrl = signal<string | null>(null);
  readonly kind = computed(() => coverKind(this.type()));
  readonly label = computed(() => typeLabel(this.type()));
  readonly short = computed(() => ({ article: 'Art.', these: 'Th.', rapport: 'Rap.', preprint: 'Pre.', autre: 'Doc.' })[this.kind()]);

  private request?: Subscription;
  private objectUrl: string | null = null;

  constructor() {
    // Une balise img ne peut pas envoyer Authorization. Les couvertures privees sont donc
    // chargees par HttpClient avec le JWT, puis affichees depuis une URL blob locale.
    effect(() => this.load(this.imageUrl()));
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
    this.revokeObjectUrl();
  }

  markFailed(): void {
    this.failedUrl.set(this.imageUrl() ?? null);
    this.revokeObjectUrl();
    this.coverUrl.set(null);
  }

  private load(source: string | null | undefined): void {
    this.request?.unsubscribe();
    this.revokeObjectUrl();
    this.coverUrl.set(null);
    this.failedUrl.set(null);
    if (!source) return;

    const match = source.match(/\/documents\/(\d+)\/image(?:[?]|$)/);
    if (!match) {
      this.coverUrl.set(source);
      return;
    }

    const documentId = Number(match[1]);
    this.request = this.api.loadCoverImage(documentId).subscribe({
      next: (image) => {
        this.objectUrl = URL.createObjectURL(image);
        this.coverUrl.set(this.objectUrl);
      },
      error: () => this.failedUrl.set(source)
    });
  }

  private revokeObjectUrl(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl = null;
  }
}

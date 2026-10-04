import { Component, computed, input } from '@angular/core';
import { coverKind, typeLabel } from '../core/labels';
import { TranslatePipe } from '../core/i18n';

/** Couverture typée : type + année dans la couleur du type. Repli quand il n'y a pas de vignette. */
@Component({
  selector: 'm-type-cover',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    @if (imageUrl()) {
      <img [class]="'m-cover m-cover--img m-cover--' + size()" [src]="imageUrl()" alt="" />
    } @else {
      <span [class]="'m-cover m-cover--' + size() + ' m-cover--' + kind()" aria-hidden="true">
        <span class="m-cover__type">{{ (size() === 'sm' ? short() : label()) | t }}</span>
        @if (size() !== 'sm' && year()) { <span class="m-cover__year">{{ year() }}</span> }
      </span>
    }
  `
})
export class TypeCoverComponent {
  readonly type = input<string | null | undefined>('');
  readonly year = input<number | string | null | undefined>('');
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly imageUrl = input<string | null | undefined>(null);
  readonly kind = computed(() => coverKind(this.type()));
  readonly label = computed(() => typeLabel(this.type()));
  readonly short = computed(() => ({ article: 'Art.', these: 'Th.', rapport: 'Rap.', preprint: 'Pre.', autre: 'Doc.' })[this.kind()]);
}

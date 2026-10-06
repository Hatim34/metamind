import { Component, computed, input } from '@angular/core';
import { confidenceLevel } from '../core/labels';
import { I18nService } from '../core/i18n';
import { inject } from '@angular/core';

/**
 * Jauge de confiance (5 barres) + libellé, seuils 0,90 / 0,70 du Business Plan.
 * Le score vient de la dernière extraction ; sans extraction, « Confiance non calculée ».
 */
@Component({
  selector: 'm-confidence',
  standalone: true,
  template: `
    <span class="m-conf" [class]="'m-conf m-conf--' + state()">
      <span class="m-conf__bars" aria-hidden="true">
        @for (bar of bars(); track $index) { <span class="m-conf__bar" [class.is-on]="bar"></span> }
      </span>
      <span class="m-conf__label">{{ label() }}</span>
    </span>
  `
})
export class ConfidenceComponent {
  private readonly i18n = inject(I18nService);
  readonly score = input<number | null | undefined>(null);
  readonly confirmed = input(false);
  readonly empty = input(false);
  readonly required = input(true);

  // Un champ vide n'est jamais « vérifié », même si le bibliothécaire l'a touché.
  readonly state = computed(() => {
    if (this.empty()) return 'low';
    if (this.confirmed()) return 'done';
    return confidenceLevel(this.score());
  });
  readonly bars = computed(() => {
    if (this.empty()) return [false, false, false, false, false];
    const filled = this.confirmed() ? 5 : this.score() == null ? 0 : Math.round((this.score() as number) * 5);
    return [0, 1, 2, 3, 4].map((i) => i < filled);
  });
  readonly label = computed(() => {
    const t = (s: string) => this.i18n.t(s);
    const pct = this.score() == null ? '' : Math.round((this.score() as number) * 100) + ' %';
    switch (this.state()) {
      case 'done': return t('Vérifié par vous');
      case 'high': return t('IA sûre à') + ' ' + pct;
      case 'mid': return t('À relire') + ' (' + pct + ')';
      case 'low': return this.empty() ? t(this.required() ? 'À compléter' : 'Vide') : t('Peu sûr, à corriger') + ' (' + pct + ')';
      default: return t('Confiance non calculée');
    }
  });
}

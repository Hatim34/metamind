import { Injectable, Pipe, PipeTransform, inject, signal } from '@angular/core';
import { NL, EN } from './translations';

export type Language = 'fr' | 'nl' | 'en';

/**
 * Traductions (N2) : le texte français sert de clé ; NL et EN dans translations.ts.
 * Détection de la langue du navigateur, choix mémorisé.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private static readonly key = 'metamind.langue';
  readonly lang = signal<Language>(this.initial());

  constructor() {
    document.documentElement.lang = this.lang();
  }

  setLanguage(lang: Language): void {
    this.lang.set(lang);
    document.documentElement.lang = lang;
    try { localStorage.setItem(I18nService.key, lang); } catch { /* ignoré */ }
  }

  t(fr: string): string {
    const lang = this.lang();
    if (lang === 'fr' || !fr) return fr;
    return (lang === 'nl' ? NL : EN)[fr] ?? fr;
  }

  private initial(): Language {
    try {
      const saved = localStorage.getItem(I18nService.key) as Language | null;
      if (saved === 'fr' || saved === 'nl' || saved === 'en') return saved;
    } catch { /* ignoré */ }
    const nav = (navigator.language || 'fr').slice(0, 2);
    return nav === 'nl' ? 'nl' : nav === 'en' ? 'en' : 'fr';
  }
}

@Pipe({ name: 't', standalone: true, pure: false })
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);
  transform(value: string | null | undefined): string {
    return this.i18n.t(value ?? '');
  }
}

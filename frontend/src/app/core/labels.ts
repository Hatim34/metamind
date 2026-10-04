import { PublicationStatus } from '../api.service';

/** Libellés humains : aucun code technique (A_VALIDER, PUBLIC...) n'est affiché tel quel. */
export const STATUS_LABELS: Record<PublicationStatus, string> = {
  EN_ATTENTE: 'Non extrait',
  EXTRACTION: 'En extraction',
  A_VALIDER: 'À valider',
  PUBLIE: 'Publié',
  SUPPRIME: 'Supprimé'
};

export const VISIBILITY_LABELS: Record<'PUBLIC' | 'INSTITUTION', string> = {
  PUBLIC: 'Public',
  INSTITUTION: 'Institution'
};

export const LANGUAGE_LABELS: Record<string, string> = {
  fr: 'Français', nl: 'Néerlandais', en: 'Anglais', de: 'Allemand', es: 'Espagnol', it: 'Italien'
};

export function languageLabel(code?: string | null): string {
  if (!code) {
    return '';
  }
  return LANGUAGE_LABELS[code.toLowerCase()] ?? code;
}

/** Couleurs des couvertures typées (repli quand il n'y a pas de vignette). */
export type CoverKind = 'article' | 'these' | 'rapport' | 'preprint' | 'autre';

export function coverKind(type?: string | null): CoverKind {
  const value = (type ?? '').toLowerCase();
  if (value.includes('th')) return 'these';
  if (value.includes('rap') || value.includes('report')) return 'rapport';
  if (value.includes('pre')) return 'preprint';
  if (value.includes('art')) return 'article';
  return 'autre';
}

export function typeLabel(type?: string | null): string {
  const labels: Record<CoverKind, string> = { article: 'Article', these: 'Thèse', rapport: 'Rapport', preprint: 'Preprint', autre: 'Document' };
  return type && coverKind(type) === 'autre' ? type : labels[coverKind(type)];
}

/** Seuils de confiance du Business Plan (livrable 04). */
export function confidenceLevel(score: number | null | undefined): 'high' | 'mid' | 'low' | 'unknown' {
  if (score === null || score === undefined) return 'unknown';
  if (score >= 0.9) return 'high';
  if (score >= 0.7) return 'mid';
  return 'low';
}

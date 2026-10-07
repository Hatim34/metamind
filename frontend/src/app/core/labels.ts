import { PublicationStatus } from '../api.service';

/** Libellés humains : aucun code technique (A_VALIDER, PUBLIC...) n'est affiché tel quel. */
export const STATUS_LABELS: Record<PublicationStatus, string> = {
  EN_ATTENTE: 'Non extrait',
  EXTRACTION: 'En cours',
  A_VALIDER: 'À valider',
  REJETE: 'Rejeté',
  ECHEC: 'Fichier illisible',
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

/** Vocabulaire de référence du serveur (tables langues et types_documents). */
export const DOCUMENT_TYPES: Record<string, string> = {
  article: 'Article', these: 'Thèse', memoire: 'Mémoire', rapport: 'Rapport', chapitre: 'Chapitre',
  communication: 'Communication', preprint: 'Preprint', autre: 'Document'
};
export const REFERENCE_LANGUAGES = ['fr', 'nl', 'en'];

export function typeLabel(type?: string | null): string {
  const known = DOCUMENT_TYPES[(type ?? '').toLowerCase()];
  if (known) return known;
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

/** Les 26 domaines OpenAlex arrivent toujours en anglais : traduits ici, sans appel au modèle. */
const CLASSIFICATIONS: Record<string, { fr: string; nl: string }> = {
  'Agricultural and Biological Sciences': { fr: 'Sciences agronomiques et biologiques', nl: 'Landbouw- en biologische wetenschappen' },
  'Arts and Humanities': { fr: 'Arts et sciences humaines', nl: 'Kunsten en geesteswetenschappen' },
  'Biochemistry, Genetics and Molecular Biology': { fr: 'Biochimie, génétique et biologie moléculaire', nl: 'Biochemie, genetica en moleculaire biologie' },
  'Business, Management and Accounting': { fr: 'Gestion, management et comptabilité', nl: 'Bedrijfskunde, management en boekhouding' },
  'Chemical Engineering': { fr: 'Génie chimique', nl: 'Chemische technologie' },
  'Chemistry': { fr: 'Chimie', nl: 'Scheikunde' },
  'Computer Science': { fr: 'Informatique', nl: 'Informatica' },
  'Decision Sciences': { fr: 'Sciences de la décision', nl: 'Beslissingswetenschappen' },
  'Dentistry': { fr: 'Dentisterie', nl: 'Tandheelkunde' },
  'Earth and Planetary Sciences': { fr: 'Sciences de la Terre et des planètes', nl: 'Aard- en planetaire wetenschappen' },
  'Economics, Econometrics and Finance': { fr: 'Économie, économétrie et finance', nl: 'Economie, econometrie en financiën' },
  'Energy': { fr: 'Énergie', nl: 'Energie' },
  'Engineering': { fr: 'Ingénierie', nl: 'Ingenieurswetenschappen' },
  'Environmental Science': { fr: 'Sciences de l\'environnement', nl: 'Milieuwetenschappen' },
  'Health Professions': { fr: 'Professions de santé', nl: 'Gezondheidsberoepen' },
  'Immunology and Microbiology': { fr: 'Immunologie et microbiologie', nl: 'Immunologie en microbiologie' },
  'Materials Science': { fr: 'Science des matériaux', nl: 'Materiaalkunde' },
  'Mathematics': { fr: 'Mathématiques', nl: 'Wiskunde' },
  'Medicine': { fr: 'Médecine', nl: 'Geneeskunde' },
  'Neuroscience': { fr: 'Neurosciences', nl: 'Neurowetenschappen' },
  'Nursing': { fr: 'Soins infirmiers', nl: 'Verpleegkunde' },
  'Pharmacology, Toxicology and Pharmaceutics': { fr: 'Pharmacologie, toxicologie et pharmacie', nl: 'Farmacologie, toxicologie en farmaceutica' },
  'Physics and Astronomy': { fr: 'Physique et astronomie', nl: 'Natuurkunde en sterrenkunde' },
  'Psychology': { fr: 'Psychologie', nl: 'Psychologie' },
  'Social Sciences': { fr: 'Sciences sociales', nl: 'Sociale wetenschappen' },
  'Veterinary': { fr: 'Médecine vétérinaire', nl: 'Diergeneeskunde' }
};

export function classificationLabel(value: string | null | undefined, lang: string): string {
  if (!value) return '';
  const known = CLASSIFICATIONS[value.trim()];
  return known && (lang === 'fr' || lang === 'nl') ? known[lang] : value;
}

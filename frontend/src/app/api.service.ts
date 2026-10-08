import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, of, throwError, timer } from 'rxjs';
import { first, map, retry, switchMap, take } from 'rxjs/operators';

export interface Publication {
  id: number;
  title: string;
  author: string;
  institution: string;
  year: number;
  summary?: string | null;
  publicationDate?: string | null;
  classification?: string | null;
  language?: string | null;
  documentType?: string | null;
  status: PublicationStatus;
  visibility: 'PUBLIC' | 'INSTITUTION';
  keywords: string[];
  imageUrl?: string | null;
  fileUrl?: string | null;
  /** Texte du fichier lu : le document peut être analysé par l'IA. */
  textReady?: boolean;
}

/** Traduction de consultation : la notice source, les auteurs et la citation ne changent jamais. */
export interface PublicationTranslation {
  language: 'fr' | 'nl' | 'en';
  sourceLanguage: string | null;
  title: string;
  summary: string | null;
  keywords: string[];
  translated: boolean;
  classification?: string | null;
}

export type PublicationStatus = 'EN_ATTENTE' | 'EXTRACTION' | 'A_VALIDER' | 'REJETE' | 'ECHEC' | 'PUBLIE' | 'SUPPRIME';

export interface UserSession {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  institution: string;
  status: 'EN_ATTENTE' | 'ACTIF' | 'DESACTIVE';
}

export interface Institution {
  id: number;
  code: string;
  name: string;
  emailDomain: string;
  active: boolean;
  /** Demandée à l'inscription, en attente de la décision de l'administrateur. */
  pending: boolean;
  creditBalance: number;
}

export interface InstitutionPhoto {
  institutionId: number;
  name: string;
  photoUrl: string;
  credit: string;
}

export interface CreditBalance {
  institutionId: number;
  institution: string;
  balance: number;
}

export interface CreditMovement {
  id: number;
  institution: string;
  type: 'ACHAT' | 'CONSOMMATION';
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface CreditAccount {
  balance: CreditBalance;
  movements: CreditMovement[];
}

export interface CreditPackOption {
  id: number;
  credits: number;
  amount: number;
  currency: string;
  label: string;
}

export interface CreditCheckout {
  checkout_url: string;
  reference: string;
}

export interface CreditCheckoutStatus {
  reference: string;
  status: string;
  balance: number;
}

export interface DashboardStatistics {
  scope: string;
  totalPublications: number;
  publishedPublications: number;
  pendingValidationPublications: number;
  publicPublications: number;
  institutionOnlyPublications: number;
  creditBalance: number;
  validationRate?: number;
  rejectionRate?: number;
  averageProcessingHours?: number;
  averageProcessingMinutes?: number;
  documentTypeDistribution?: Record<string, number>;
  classificationDistribution?: Record<string, number>;
}

export interface MetadataExtraction {
  publicationId: number;
  title: string;
  suggestedTitle: string;
  suggestedAuthor: string;
  suggestedKeywords: string[];
  creditBalance: number;
}

export interface MetadataAuthor {
  nom_complet: string;
  orcid?: string;
}

export interface MetadataDetails {
  id: number;
  document_id: number;
  titre: string;
  resume: string;
  date_publication: string | null;
  classification: string;
  visibilite: 'PUBLIC' | 'INSTITUTION';
  statut: 'EN_ATTENTE' | 'VALIDE';
  date_validation: string | null;
  validee_par: number | null;
  auteurs: MetadataAuthor[];
  mots_cles: string[];
  langue?: string | null;
  type_document?: string | null;
  doi?: string | null;
  texte_extrait?: string | null;
  confiances?: Record<string, number>;
}

export interface MetadataValidationRequest {
  titre: string;
  resume: string;
  date_publication: string | null;
  classification: string;
  visibilite: 'PUBLIC' | 'INSTITUTION';
  auteurs: MetadataAuthor[];
  mots_cles: string[];
  langue: string | null;
  type_document: string | null;
  doi: string | null;
}

export interface ReferenceValue {
  code: string;
  libelle: string;
}

export interface ReferenceData {
  langues: ReferenceValue[];
  types_documents: ReferenceValue[];
}

export interface ExtractionFieldQuality {
  champ: string;
  arbitrees: number;
  acceptees: number;
  modifiees: number;
  videes: number;
  rejetees: number;
  taux_acceptation: number;
  distance_edition_moyenne: number;
}

export interface ExtractionCalibrationBucket {
  tranche: string;
  arbitrees: number;
  acceptees: number;
  taux_acceptation: number;
}

export interface ExtractionQuality {
  scope: string;
  suggestions_arbitrees: number;
  taux_acceptation_global: number;
  par_champ: ExtractionFieldQuality[];
  calibration_score: ExtractionCalibrationBucket[];
}

export interface AuthResponse {
  token: string;
  user: UserSession;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  institution: string;
  password: string;
  /** Nom de l'institution à ajouter quand le domaine de l'adresse n'est pas encore inscrit. */
  nom_institution?: string;
  /** Langue de l'interface : celle des emails envoyés au compte. */
  langue?: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  institution: string;
}

export interface CreateInstitutionRequest {
  code: string;
  name: string;
  emailDomain: string;
}

export interface CreatePublicationRequest {
  title: string;
  author: string;
  institution: string;
  year: number;
  visibility: 'PUBLIC' | 'INSTITUTION';
  keywords: string[];
  image?: File | null;
}

export interface SearchFilters {
  author?: string;
  language?: string;
  documentType?: string;
  startDate?: string;
  endDate?: string;
  /** Langue d'affichage : le serveur renvoie titres, résumés et mots-clés traduits s'ils sont prêts. */
  display?: string;
}

export interface UpdatePublicationStatusRequest {
  status: Extract<PublicationStatus, 'A_VALIDER' | 'PUBLIE' | 'SUPPRIME'>;
}

export interface AdminUserUpdateRequest {
  role?: 'LIBRARIAN' | 'ADMIN';
  statut?: 'EN_ATTENTE' | 'ACTIF' | 'DESACTIVE';
}

export interface AuditLog {
  id: number;
  action: string;
  type_entite: string;
  entite_id: number | null;
  libelle_entite: string | null;
  auteur: string | null;
  details: string;
  date_creation: string;
}

export interface PageResponse<T> {
  contenu: T[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = this.resolveBaseUrl();
  private token = '';

  constructor(private readonly http: HttpClient) {}

  setToken(token: string): void {
    this.token = token;
  }

  getPublications(search = ''): Observable<Publication[]> {
    const params = search.trim() ? new HttpParams().set('search', search.trim()) : undefined;
    return this.http.get<unknown[]>(`${this.baseUrl}/publications`, { params, headers: this.optionalAuthHeaders() })
      .pipe(map((response) => response.map((item) => this.toPublication(item))));
  }

  getPublication(publicationId: number): Observable<Publication> {
	return this.http.get<unknown>(`${this.baseUrl}/publications/${publicationId}`, { headers: this.optionalAuthHeaders() })
	  .pipe(map((response) => this.toPublication(response)));
  }

  /** `pending` : la traduction est en préparation (202), la notice d'origine est renvoyée en attendant. */
  getPublicationTranslation(publicationId: number, language: 'fr' | 'nl' | 'en'): Observable<{ translation: PublicationTranslation; pending: boolean }> {
    return this.http.get<unknown>(`${this.baseUrl}/publications/${publicationId}/traduction`, {
      headers: this.optionalAuthHeaders(),
      params: new HttpParams().set('langue', language),
      observe: 'response'
    }).pipe(map((response) => ({ translation: this.toPublicationTranslation(response.body), pending: response.status === 202 })));
  }

  downloadPublicationFile(publicationId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/documents/${publicationId}/file`, {
      // Une fiche publique doit rester lisible sans session. Envoyer « Bearer » vide
      // force le backend a tenter une authentification et transforme ce cas en 401.
      headers: this.optionalAuthHeaders(),
      responseType: 'blob'
    });
  }

  /** Remplace l'image de la notice (photo ou figure de l'article) ; renvoie la publication mise à jour. */
  replaceDocumentImage(publicationId: number, image: File): Observable<Publication> {
    const data = new FormData();
    data.append('image', image);
    return this.http.put<unknown>(`${this.baseUrl}/documents/${publicationId}/image`, data, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  loadCoverImage(publicationId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/documents/${publicationId}/image`, {
      headers: this.optionalAuthHeaders(),
      responseType: 'blob'
    });
  }

  retryDocumentProcessing(publicationId: number): Observable<Publication> {
    return this.http.post<unknown>(`${this.baseUrl}/documents/${publicationId}/processing`, {}, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/password-reset/request`, { email });
  }

  confirmPasswordReset(token: string, password: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/password-reset/confirm`, { token, password });
  }

  searchPublications(search = '', filters: SearchFilters = {}): Observable<Publication[]> {
    let params = new HttpParams();
    if (search.trim()) {
      params = params.set('q', search.trim());
    }
    if (filters.author?.trim()) {
      params = params.set('author', filters.author.trim());
    }
    if (filters.language?.trim()) {
      params = params.set('langue', filters.language.trim());
    }
    if (filters.documentType?.trim()) {
      params = params.set('type', filters.documentType.trim());
    }
    if (filters.startDate) {
      params = params.set('date_debut', filters.startDate);
    }
    if (filters.endDate) {
      params = params.set('date_fin', filters.endDate);
    }
    if (filters.display) {
      params = params.set('affichage', filters.display);
    }
    return this.allPages(`${this.baseUrl}/search`, params)
      .pipe(map((items) => items.map((item) => this.toPublication(item))));
  }

  getManagedDocuments(status?: PublicationStatus): Observable<Publication[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('statut', status);
    }
    return this.allPages(`${this.baseUrl}/documents`, params, this.authHeaders())
      .pipe(map((items) => items.map((item) => this.toPublication(item))));
  }

  /**
   * Toutes les pages d'une liste paginee. L'API plafonne une page a 100 elements, et le
   * catalogue filtre et compte cote client : lire la premiere page seule cachait la
   * plupart des documents (20 publications affichees sur 103).
   */
  private allPages(url: string, params: HttpParams, headers?: HttpHeaders): Observable<unknown[]> {
    const pageParams = (page: number) => params.set('size', '100').set('page', String(page));
    return this.http.get<PageResponse<unknown>>(url, { params: pageParams(0), headers }).pipe(
      switchMap((firstPage) => firstPage.total_pages <= 1
        ? of(firstPage.contenu)
        : forkJoin(Array.from({ length: firstPage.total_pages - 1 }, (_, index) =>
            this.http.get<PageResponse<unknown>>(url, { params: pageParams(index + 1), headers })))
            .pipe(map((pages) => [...firstPage.contenu, ...pages.flatMap((page) => page.contenu)])))
    );
  }

  createPublication(request: CreatePublicationRequest): Observable<Publication> {
    const data = new FormData();
    data.append('title', request.title);
    data.append('author', request.author);
    data.append('year', String(request.year));
    data.append('visibility', request.visibility);
    request.keywords.forEach((keyword) => data.append('keywords', keyword));
    if (request.image) {
      data.append('image', request.image);
    }
    return this.http.post<unknown>(`${this.baseUrl}/publications`, data, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  importDocument(file: File, visibility: 'PUBLIC' | 'INSTITUTION', image?: File | null): Observable<Publication> {
    const data = new FormData();
    data.append('fichier', file);
    data.append('visibilite', visibility);
    if (image) {
      data.append('image', image);
    }
    return this.http.post<unknown>(`${this.baseUrl}/documents`, data, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  updatePublicationStatus(publicationId: number, request: UpdatePublicationStatusRequest): Observable<Publication> {
    return this.http.put<unknown>(`${this.baseUrl}/publications/${publicationId}/status`, request, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  deletePublication(publicationId: number): Observable<Publication> {
    return this.http.delete<unknown>(`${this.baseUrl}/publications/${publicationId}`, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  /** Photos et crédits des institutions, choisis par l'administrateur (public). */
  getInstitutionPhotos(): Observable<InstitutionPhoto[]> {
    return this.http.get<Record<string, any>[]>(`${this.baseUrl}/institutions/photos`).pipe(map((items) => items.map((item) => ({
      institutionId: item['institution_id'],
      name: item['nom'],
      photoUrl: this.toResourceUrl(item['photo_url']) ?? '',
      credit: item['credit'] ?? ''
    }))));
  }

  uploadInstitutionPhoto(institutionId: number, image: File, credit: string): Observable<unknown> {
    const data = new FormData();
    data.append('image', image);
    data.append('credit', credit);
    return this.http.put(`${this.baseUrl}/institutions/${institutionId}/photo`, data, { headers: this.authHeaders() });
  }

  getInstitutions(): Observable<Institution[]> {
    return this.http.get<unknown[]>(`${this.baseUrl}/institutions`, { headers: this.authHeaders() })
      .pipe(map((response) => response.map((item) => this.toInstitution(item))));
  }

  createInstitution(request: CreateInstitutionRequest): Observable<Institution> {
    return this.http.post<unknown>(`${this.baseUrl}/institutions`, request, { headers: this.authHeaders() })
      .pipe(map((response) => this.toInstitution(response)));
  }

  deactivateInstitution(institutionId: number): Observable<Institution> {
    return this.http.delete<unknown>(`${this.baseUrl}/institutions/${institutionId}`, { headers: this.authHeaders() })
      .pipe(map((response) => this.toInstitution(response)));
  }

  getCreditAccount(): Observable<CreditAccount> {
    return this.http.get<unknown>(`${this.baseUrl}/credits`, { headers: this.authHeaders() })
      .pipe(map((response) => this.toCreditAccount(response)));
  }

  getCreditPacks(): Observable<CreditPackOption[]> {
    return this.http.get<unknown[]>(`${this.baseUrl}/credits/packs`)
      .pipe(map((response) => response.map((item) => this.toCreditPack(item))));
  }

  startCreditCheckout(packId: number, termsAccepted: boolean, withdrawalWaiverAccepted: boolean): Observable<CreditCheckout> {
    return this.http.post<CreditCheckout>(`${this.baseUrl}/credits`, {
      pack_id: packId,
      cgv_acceptees: termsAccepted,
      renonciation_retractation_acceptee: withdrawalWaiverAccepted
    }, { headers: this.authHeaders() });
  }

  getCreditCheckoutStatus(reference: string): Observable<CreditCheckoutStatus> {
    return this.http.get<unknown>(`${this.baseUrl}/credits/checkout/${encodeURIComponent(reference)}`, { headers: this.authHeaders() })
      .pipe(map((response) => {
        const value = response as { reference: string; status: string; solde_credits: number };
        return { reference: value.reference, status: value.status, balance: value.solde_credits };
      }));
  }

  getCreditBalance(userId: number): Observable<CreditBalance> {
    return this.http.get<unknown>(`${this.baseUrl}/users/${userId}/credits`, { headers: this.authHeaders() })
      .pipe(map((response) => this.toCreditBalance(response)));
  }

  getCreditMovements(userId: number): Observable<CreditMovement[]> {
    return this.http.get<unknown[]>(`${this.baseUrl}/users/${userId}/credits/movements`, { headers: this.authHeaders() })
      .pipe(map((response) => response.map((item) => this.toCreditMovement(item))));
  }

  getStatistics(): Observable<DashboardStatistics> {
    return this.http.get<unknown>(`${this.baseUrl}/stats`, { headers: this.authHeaders() })
      .pipe(map((response) => this.toStatistics(response)));
  }

  /**
   * Lance l'extraction des metadonnees par le LLM.
   *
   * Le texte du document est extrait de maniere asynchrone juste apres l'import :
   * demander l'extraction trop tot renvoie un 400 "le document ne contient pas de
   * texte extrait". Ce n'est pas un echec mais une attente, donc on repatiente
   * au lieu d'abandonner. Tous les autres refus (402 credits, 403, 404) remontent
   * immediatement a l'appelant.
   */
  extractMetadata(publicationId: number): Observable<MetadataExtraction> {
    return this.http.post<unknown>(`${this.baseUrl}/publications/${publicationId}/extraction`, {}, { headers: this.authHeaders() })
      .pipe(
        retry({
          count: ApiService.extractionAttempts,
          delay: (error: unknown, attempt: number) =>
            ApiService.isTextNotReady(error) ? timer(2000 * attempt) : throwError(() => error)
        }),
        map((response) => this.toMetadataExtraction(response))
      );
  }

  /**
   * Attend que le texte du document soit disponible, puis lance l'analyse du LLM.
   *
   * L'import declenche une extraction de texte asynchrone. Appeler l'extraction
   * aussitot provoque un refus previsible (400), visible en rouge dans la console
   * du navigateur meme lorsque l'application le rattrape. Interroger le statut
   * evite ce refus : on n'appelle le LLM que lorsque le document est pret.
   */
  extractMetadataWhenReady(publicationId: number): Observable<MetadataExtraction> {
    return this.waitForText(publicationId).pipe(
      switchMap(() => this.extractMetadata(publicationId))
    );
  }

  /**
   * Interroge le document jusqu'a ce que son texte soit lu, ou que le fichier soit declare illisible.
   * Renvoie null si l'attente expire : le reessai de extractMetadata prend alors le relais.
   */
  waitForText(publicationId: number): Observable<Publication | null> {
    return timer(0, 1500).pipe(
      take(ApiService.readinessPolls),
      switchMap(() => this.getPublication(publicationId)),
      first((document) => !!document.textReady || document.status === 'ECHEC', null)
    );
  }

  /** Une minute d'attente : un PDF de plusieurs dizaines de pages se lit en quelques secondes. */
  private static readonly readinessPolls = 40;

  /** Nombre d'attentes avant de renoncer : le texte arrive en quelques secondes. */
  private static readonly extractionAttempts = 8;

  private static isTextNotReady(error: unknown): boolean {
    const response = error as { status?: number; error?: { message?: string } };
    return response?.status === 400 && /texte extrait/i.test(response?.error?.message ?? '');
  }

  exportPersonalData(userId: number): Observable<unknown> {
    return this.http.get<unknown>(`${this.baseUrl}/users/${userId}/data-export`, { headers: this.authHeaders() });
  }

  getMetadata(publicationId: number): Observable<MetadataDetails> {
    return this.http.get<MetadataDetails>(`${this.baseUrl}/documents/${publicationId}/metadata`, { headers: this.authHeaders() });
  }

  validateMetadata(publicationId: number, request: MetadataValidationRequest): Observable<MetadataDetails> {
    return this.http.put<MetadataDetails>(`${this.baseUrl}/documents/${publicationId}/metadata`, request, { headers: this.authHeaders() });
  }

  getReferences(): Observable<ReferenceData> {
    return this.http.get<ReferenceData>(`${this.baseUrl}/references`);
  }

  getExtractionQuality(): Observable<ExtractionQuality> {
    return this.http.get<ExtractionQuality>(`${this.baseUrl}/stats/qualite-extraction`, { headers: this.authHeaders() });
  }

  rejectMetadata(publicationId: number, reason: string): Observable<unknown> {
    return this.http.post<unknown>(`${this.baseUrl}/documents/${publicationId}/metadata/rejet`, { motif: reason }, { headers: this.authHeaders() });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<unknown>(`${this.baseUrl}/auth/login`, request)
      .pipe(map((response) => this.toAuthResponse(response)));
  }

  register(request: RegisterRequest): Observable<{ statut: string; message: string }> {
    return this.http.post<{ statut: string; message: string }>(`${this.baseUrl}/auth/register`, request);
  }

  updateProfile(userId: number, request: UpdateProfileRequest): Observable<UserSession> {
    return this.http.put<unknown>(`${this.baseUrl}/users/${userId}/profile`, request, { headers: this.authHeaders() })
      .pipe(map((response) => this.toUserSession(response)));
  }

  changePassword(userId: number, currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/users/${userId}/password`,
      { mot_de_passe_actuel: currentPassword, nouveau_mot_de_passe: newPassword }, { headers: this.authHeaders() });
  }

  /** Valide (true) ou refuse/désactive (false) une institution ; valider une demande active aussi ses comptes. */
  setInstitutionActive(institutionId: number, active: boolean): Observable<Institution> {
    return this.http.patch<unknown>(`${this.baseUrl}/admin/institutions/${institutionId}`, { actif: active }, { headers: this.authHeaders() })
      .pipe(map((response) => this.toInstitution(response)));
  }

  adjustInstitutionCredits(institutionId: number, amount: number, reason: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/admin/institutions/${institutionId}/credits/adjustments`, { amount, reason }, { headers: this.authHeaders() });
  }

  requestAccountDeletion(userId: number): Observable<UserSession> {
    return this.http.delete<unknown>(`${this.baseUrl}/users/${userId}`, { headers: this.authHeaders() })
      .pipe(map((response) => this.toUserSession(response)));
  }

  getAdminUsers(institutionId?: number): Observable<UserSession[]> {
    const params = institutionId ? new HttpParams().set('institutionId', institutionId) : undefined;
    return this.http.get<PageResponse<UserSession>>(`${this.baseUrl}/admin/users`, { params, headers: this.authHeaders() })
      .pipe(map((response) => response.contenu.map((user) => this.toUserSession(user))));
  }

  updateAdminUser(userId: number, request: AdminUserUpdateRequest): Observable<UserSession> {
    return this.http.patch<unknown>(`${this.baseUrl}/admin/users/${userId}`, request, { headers: this.authHeaders() })
      .pipe(map((response) => this.toUserSession(response)));
  }

  getAdminConfig(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.baseUrl}/admin/config`, { headers: this.authHeaders() });
  }

  updateAdminConfig(config: Record<string, string>): Observable<Record<string, string>> {
    return this.http.patch<Record<string, string>>(`${this.baseUrl}/admin/config`, config, { headers: this.authHeaders() });
  }

  exportAdminDocumentsCsv(): Observable<string> {
    return this.http.get(`${this.baseUrl}/admin/reports/documents.csv`, {
      headers: this.authHeaders(),
      responseType: 'text'
    });
  }

  getAdminLogs(page: number, query: string): Observable<PageResponse<AuditLog>> {
    const params = new HttpParams().set('page', page).set('size', 50).set('q', query);
    return this.http.get<PageResponse<AuditLog>>(`${this.baseUrl}/admin/logs`, { headers: this.authHeaders(), params });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.token}` });
  }

  private optionalAuthHeaders(): HttpHeaders | undefined {
    return this.token ? this.authHeaders() : undefined;
  }

  private toPublication(value: unknown): Publication {
    const item = value as Record<string, any>;
    return {
      id: item['id'],
      title: item['titre'] ?? item['title'],
      author: item['auteur'] ?? item['author'],
      institution: item['institution'],
      year: item['annee'] ?? item['year'],
      summary: item['resume'] ?? item['summary'] ?? null,
      publicationDate: item['date_publication'] ?? item['publicationDate'] ?? null,
      classification: item['classification'] ?? null,
      language: item['langue'] ?? item['language'] ?? null,
      documentType: item['type_document'] ?? item['documentType'] ?? null,
      status: item['statut'] ?? item['status'],
      visibility: item['visibilite'] ?? item['visibility'],
      keywords: item['mots_cles'] ?? item['keywords'] ?? [],
      imageUrl: this.toResourceUrl(item['image_url'] ?? item['imageUrl']),
      fileUrl: this.toResourceUrl(item['fichier_url'] ?? item['fileUrl']),
      textReady: item['texte_pret'] ?? false
    };
  }

  private toPublicationTranslation(value: unknown): PublicationTranslation {
    const item = value as Record<string, any>;
    return {
      language: item['langue'] ?? item['language'],
      sourceLanguage: item['langue_source'] ?? item['sourceLanguage'] ?? null,
      title: item['titre'] ?? item['title'],
      summary: item['resume'] ?? item['summary'] ?? null,
      keywords: item['mots_cles'] ?? item['keywords'] ?? [],
      translated: item['traduite'] ?? item['translated'] ?? false,
      classification: item['classification'] ?? null
    };
  }

  private toResourceUrl(value: unknown): string | null {
    if (typeof value !== 'string' || !value.trim()) {
      return null;
    }
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return value;
    }
    if (this.baseUrl.startsWith('http://localhost:8080') && value.startsWith('/api/')) {
      return `http://localhost:8080${value}`;
    }
    return value;
  }

  private toUserSession(value: unknown): UserSession {
    const item = value as Record<string, any>;
    return {
      id: item['id'],
      firstName: item['prenom'] ?? item['firstName'],
      lastName: item['nom'] ?? item['lastName'],
      email: item['email'],
      role: item['role'],
      institution: item['institution'],
      status: item['statut'] ?? item['status']
    };
  }

  private toAuthResponse(value: unknown): AuthResponse {
    const item = value as Record<string, any>;
    return {
      token: item['token'],
      user: this.toUserSession(item['utilisateur'] ?? item['user'])
    };
  }

  private toInstitution(value: unknown): Institution {
    const item = value as Record<string, any>;
    return {
      id: item['id'],
      code: item['code'],
      name: item['nom'] ?? item['name'],
      emailDomain: item['domaine_email'] ?? item['emailDomain'],
      active: item['actif'] ?? item['active'],
      pending: item['en_attente'] ?? false,
      creditBalance: item['solde_credits'] ?? 0
    };
  }

  private toCreditBalance(value: unknown): CreditBalance {
    const item = value as Record<string, any>;
    return {
      institutionId: item['institution_id'] ?? item['institutionId'],
      institution: item['institution'],
      balance: item['solde_credits'] ?? item['balance']
    };
  }

  private toCreditMovement(value: unknown): CreditMovement {
    const item = value as Record<string, any>;
    return {
      id: item['id'],
      institution: item['institution'],
      type: item['type'],
      amount: item['montant'] ?? item['amount'],
      balanceAfter: item['solde_apres'] ?? item['balanceAfter'],
      description: item['description'],
      createdAt: item['date_creation'] ?? item['createdAt']
    };
  }

  private toCreditAccount(value: unknown): CreditAccount {
    const item = value as Record<string, any>;
    return {
      balance: this.toCreditBalance(item['solde'] ?? item['balance']),
      movements: (item['mouvements'] ?? item['movements'] ?? []).map((movement: unknown) => this.toCreditMovement(movement))
    };
  }

  private toCreditPack(value: unknown): CreditPackOption {
    const item = value as Record<string, any>;
    return {
      id: item['id'],
      credits: item['quantite'] ?? item['credits'],
      amount: item['montant_paye'] ?? item['amount'],
      currency: item['devise'] ?? item['currency'],
      label: item['libelle'] ?? item['label']
    };
  }

  private toStatistics(value: unknown): DashboardStatistics {
    const item = value as Record<string, any>;
    return {
      scope: item['scope'],
      totalPublications: item['total_publications'] ?? item['totalPublications'],
      publishedPublications: item['publications_publiees'] ?? item['publishedPublications'],
      pendingValidationPublications: item['publications_a_valider'] ?? item['pendingValidationPublications'],
      publicPublications: item['publications_publiques'] ?? item['publicPublications'],
      institutionOnlyPublications: item['publications_institution'] ?? item['institutionOnlyPublications'],
      creditBalance: item['solde_credits'] ?? item['creditBalance'],
      validationRate: item['taux_validation'] ?? item['validationRate'],
      rejectionRate: item['taux_rejet'] ?? item['rejectionRate'],
      averageProcessingHours: item['temps_moyen_traitement_heures'] ?? item['averageProcessingHours'],
      averageProcessingMinutes: item['temps_moyen_traitement_minutes'] ?? (item['temps_moyen_traitement_heures'] != null ? Math.round(item['temps_moyen_traitement_heures'] * 60) : undefined),
      documentTypeDistribution: item['distribution_types_documents'] ?? item['documentTypeDistribution'],
      classificationDistribution: item['distribution_classifications'] ?? item['classificationDistribution']
    };
  }

  private toMetadataExtraction(value: unknown): MetadataExtraction {
    const item = value as Record<string, any>;
    return {
      publicationId: item['publication_id'] ?? item['publicationId'],
      title: item['titre'] ?? item['title'],
      suggestedTitle: item['titre_suggere'] ?? item['suggestedTitle'],
      suggestedAuthor: item['auteur_suggere'] ?? item['suggestedAuthor'],
      suggestedKeywords: item['mots_cles_suggeres'] ?? item['suggestedKeywords'] ?? [],
      creditBalance: item['solde_credits'] ?? item['creditBalance']
    };
  }

  private resolveBaseUrl(): string {
    if (globalThis.location?.origin === 'http://localhost:4200') {
      return 'http://localhost:8080/api/v1';
    }
    return '/api/v1';
  }
}

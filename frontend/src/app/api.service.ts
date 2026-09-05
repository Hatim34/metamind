import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface Publication {
  id: number;
  title: string;
  author: string;
  institution: string;
  year: number;
  status: PublicationStatus;
  visibility: 'PUBLIC' | 'INSTITUTION';
  keywords: string[];
}

export type PublicationStatus = 'EN_ATTENTE' | 'EXTRACTION' | 'A_VALIDER' | 'PUBLIE' | 'SUPPRIME';

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

export interface DashboardStatistics {
  scope: string;
  totalPublications: number;
  publishedPublications: number;
  pendingValidationPublications: number;
  publicPublications: number;
  institutionOnlyPublications: number;
  creditBalance: number;
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
}

export interface MetadataValidationRequest {
  titre: string;
  resume: string;
  date_publication: string | null;
  classification: string;
  visibilite: 'PUBLIC' | 'INSTITUTION';
  auteurs: MetadataAuthor[];
  mots_cles: string[];
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

  createPublication(request: CreatePublicationRequest): Observable<Publication> {
    return this.http.post<unknown>(`${this.baseUrl}/publications`, request, { headers: this.authHeaders() })
      .pipe(map((response) => this.toPublication(response)));
  }

  importDocument(file: File, visibility: 'PUBLIC' | 'INSTITUTION'): Observable<Publication> {
    const data = new FormData();
    data.append('fichier', file);
    data.append('visibilite', visibility);
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

  startCreditCheckout(packId: number): Observable<CreditCheckout> {
    return this.http.post<CreditCheckout>(`${this.baseUrl}/credits`, { pack_id: packId, cgv_acceptees: true }, { headers: this.authHeaders() });
  }

  confirmCreditPayment(reference: string): Observable<CreditBalance> {
    return this.http.post<unknown>(`${this.baseUrl}/webhooks/stripe`, { reference, type: 'checkout.session.completed' })
      .pipe(map((response) => this.toCreditBalance(response)));
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

  purchaseCredits(userId: number, amount: number): Observable<CreditBalance> {
    return this.http.post<unknown>(`${this.baseUrl}/users/${userId}/credits/purchase`, { amount }, { headers: this.authHeaders() })
      .pipe(map((response) => this.toCreditBalance(response)));
  }

  extractMetadata(publicationId: number): Observable<MetadataExtraction> {
    return this.http.post<unknown>(`${this.baseUrl}/publications/${publicationId}/extraction`, {}, { headers: this.authHeaders() })
      .pipe(map((response) => this.toMetadataExtraction(response)));
  }

  getMetadata(publicationId: number): Observable<MetadataDetails> {
    return this.http.get<MetadataDetails>(`${this.baseUrl}/documents/${publicationId}/metadata`, { headers: this.authHeaders() });
  }

  validateMetadata(publicationId: number, request: MetadataValidationRequest): Observable<MetadataDetails> {
    return this.http.put<MetadataDetails>(`${this.baseUrl}/documents/${publicationId}/metadata`, request, { headers: this.authHeaders() });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<unknown>(`${this.baseUrl}/auth/login`, request)
      .pipe(map((response) => this.toAuthResponse(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<unknown>(`${this.baseUrl}/auth/register`, request)
      .pipe(map((response) => this.toAuthResponse(response)));
  }

  updateProfile(userId: number, request: UpdateProfileRequest): Observable<UserSession> {
    return this.http.put<unknown>(`${this.baseUrl}/users/${userId}/profile`, request, { headers: this.authHeaders() })
      .pipe(map((response) => this.toUserSession(response)));
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

  getAdminLogs(): Observable<AuditLog[]> {
    return this.http.get<PageResponse<AuditLog>>(`${this.baseUrl}/admin/logs`, { headers: this.authHeaders() })
      .pipe(map((response) => response.contenu));
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
      status: item['statut'] ?? item['status'],
      visibility: item['visibilite'] ?? item['visibility'],
      keywords: item['mots_cles'] ?? item['keywords'] ?? []
    };
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
      active: item['actif'] ?? item['active']
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
      creditBalance: item['solde_credits'] ?? item['creditBalance']
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

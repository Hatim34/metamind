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
    return this.http.get<Publication[]>(`${this.baseUrl}/publications`, { params, headers: this.optionalAuthHeaders() });
  }

  createPublication(request: CreatePublicationRequest): Observable<Publication> {
    return this.http.post<Publication>(`${this.baseUrl}/publications`, request, { headers: this.authHeaders() });
  }

  importDocument(file: File, visibility: 'PUBLIC' | 'INSTITUTION'): Observable<Publication> {
    const data = new FormData();
    data.append('fichier', file);
    data.append('visibilite', visibility);
    return this.http.post<Publication>(`${this.baseUrl}/documents`, data, { headers: this.authHeaders() });
  }

  updatePublicationStatus(publicationId: number, request: UpdatePublicationStatusRequest): Observable<Publication> {
    return this.http.put<Publication>(`${this.baseUrl}/publications/${publicationId}/status`, request, { headers: this.authHeaders() });
  }

  deletePublication(publicationId: number): Observable<Publication> {
    return this.http.delete<Publication>(`${this.baseUrl}/publications/${publicationId}`, { headers: this.authHeaders() });
  }

  getInstitutions(): Observable<Institution[]> {
    return this.http.get<Institution[]>(`${this.baseUrl}/institutions`, { headers: this.authHeaders() });
  }

  createInstitution(request: CreateInstitutionRequest): Observable<Institution> {
    return this.http.post<Institution>(`${this.baseUrl}/institutions`, request, { headers: this.authHeaders() });
  }

  deactivateInstitution(institutionId: number): Observable<Institution> {
    return this.http.delete<Institution>(`${this.baseUrl}/institutions/${institutionId}`, { headers: this.authHeaders() });
  }

  getCreditAccount(): Observable<CreditAccount> {
    return this.http.get<CreditAccount>(`${this.baseUrl}/credits`, { headers: this.authHeaders() });
  }

  getCreditPacks(): Observable<CreditPackOption[]> {
    return this.http.get<CreditPackOption[]>(`${this.baseUrl}/credits/packs`);
  }

  startCreditCheckout(packId: number): Observable<CreditCheckout> {
    return this.http.post<CreditCheckout>(`${this.baseUrl}/credits`, { pack_id: packId, cgv_acceptees: true }, { headers: this.authHeaders() });
  }

  confirmCreditPayment(reference: string): Observable<CreditBalance> {
    return this.http.post<CreditBalance>(`${this.baseUrl}/webhooks/stripe`, { reference, type: 'checkout.session.completed' });
  }

  getCreditBalance(userId: number): Observable<CreditBalance> {
    return this.http.get<CreditBalance>(`${this.baseUrl}/users/${userId}/credits`, { headers: this.authHeaders() });
  }

  getCreditMovements(userId: number): Observable<CreditMovement[]> {
    return this.http.get<CreditMovement[]>(`${this.baseUrl}/users/${userId}/credits/movements`, { headers: this.authHeaders() });
  }

  getStatistics(): Observable<DashboardStatistics> {
    return this.http.get<DashboardStatistics>(`${this.baseUrl}/statistics`, { headers: this.authHeaders() });
  }

  purchaseCredits(userId: number, amount: number): Observable<CreditBalance> {
    return this.http.post<CreditBalance>(`${this.baseUrl}/users/${userId}/credits/purchase`, { amount }, { headers: this.authHeaders() });
  }

  extractMetadata(publicationId: number): Observable<MetadataExtraction> {
    return this.http.post<MetadataExtraction>(`${this.baseUrl}/publications/${publicationId}/extraction`, {}, { headers: this.authHeaders() });
  }

  getMetadata(publicationId: number): Observable<MetadataDetails> {
    return this.http.get<MetadataDetails>(`${this.baseUrl}/documents/${publicationId}/metadata`, { headers: this.authHeaders() });
  }

  validateMetadata(publicationId: number, request: MetadataValidationRequest): Observable<MetadataDetails> {
    return this.http.put<MetadataDetails>(`${this.baseUrl}/documents/${publicationId}/metadata`, request, { headers: this.authHeaders() });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request);
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, request);
  }

  updateProfile(userId: number, request: UpdateProfileRequest): Observable<UserSession> {
    return this.http.put<UserSession>(`${this.baseUrl}/users/${userId}/profile`, request, { headers: this.authHeaders() });
  }

  requestAccountDeletion(userId: number): Observable<UserSession> {
    return this.http.delete<UserSession>(`${this.baseUrl}/users/${userId}`, { headers: this.authHeaders() });
  }

  getAdminUsers(institutionId?: number): Observable<UserSession[]> {
    const params = institutionId ? new HttpParams().set('institutionId', institutionId) : undefined;
    return this.http.get<PageResponse<UserSession>>(`${this.baseUrl}/admin/users`, { params, headers: this.authHeaders() })
      .pipe(map((response) => response.contenu));
  }

  updateAdminUser(userId: number, request: AdminUserUpdateRequest): Observable<UserSession> {
    return this.http.patch<UserSession>(`${this.baseUrl}/admin/users/${userId}`, request, { headers: this.authHeaders() });
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

  private resolveBaseUrl(): string {
    if (globalThis.location?.origin === 'http://localhost:4200') {
      return 'http://localhost:8080/api/v1';
    }
    return '/api/v1';
  }
}

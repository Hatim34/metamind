import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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
  status: 'ACTIF' | 'DESACTIVE';
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

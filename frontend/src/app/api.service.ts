import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Publication {
  id: number;
  title: string;
  author: string;
  institution: string;
  year: number;
  status: string;
  visibility: 'PUBLIC' | 'INSTITUTION';
  keywords: string[];
}

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

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = this.resolveBaseUrl();

  constructor(private readonly http: HttpClient) {}

  getPublications(search = ''): Observable<Publication[]> {
    const params = search.trim() ? new HttpParams().set('search', search.trim()) : undefined;
    return this.http.get<Publication[]>(`${this.baseUrl}/publications`, { params });
  }

  createPublication(request: CreatePublicationRequest): Observable<Publication> {
    return this.http.post<Publication>(`${this.baseUrl}/publications`, request);
  }

  getInstitutions(): Observable<Institution[]> {
    return this.http.get<Institution[]>(`${this.baseUrl}/institutions`);
  }

  createInstitution(request: CreateInstitutionRequest): Observable<Institution> {
    return this.http.post<Institution>(`${this.baseUrl}/institutions`, request);
  }

  deactivateInstitution(institutionId: number): Observable<Institution> {
    return this.http.delete<Institution>(`${this.baseUrl}/institutions/${institutionId}`);
  }

  getCreditBalance(userId: number): Observable<CreditBalance> {
    return this.http.get<CreditBalance>(`${this.baseUrl}/users/${userId}/credits`);
  }

  purchaseCredits(userId: number, amount: number): Observable<CreditBalance> {
    return this.http.post<CreditBalance>(`${this.baseUrl}/users/${userId}/credits/purchase`, { amount });
  }

  extractMetadata(publicationId: number, userId: number): Observable<MetadataExtraction> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<MetadataExtraction>(`${this.baseUrl}/publications/${publicationId}/extraction`, {}, { params });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request);
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, request);
  }

  updateProfile(userId: number, request: UpdateProfileRequest): Observable<UserSession> {
    return this.http.put<UserSession>(`${this.baseUrl}/users/${userId}/profile`, request);
  }

  requestAccountDeletion(userId: number): Observable<UserSession> {
    return this.http.delete<UserSession>(`${this.baseUrl}/users/${userId}`);
  }

  private resolveBaseUrl(): string {
    if (globalThis.location?.origin === 'http://localhost:4200') {
      return 'http://localhost:8080/api/v1';
    }
    return '/api/v1';
  }
}

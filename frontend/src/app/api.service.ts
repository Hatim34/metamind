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

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  constructor(private readonly http: HttpClient) {}

  getPublications(search = ''): Observable<Publication[]> {
    const params = search.trim() ? new HttpParams().set('search', search.trim()) : undefined;
    return this.http.get<Publication[]>(`${this.baseUrl}/publications`, { params });
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
}

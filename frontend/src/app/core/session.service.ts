import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService, AuthResponse, LoginRequest, UserSession } from '../api.service';

/** Session utilisateur partagée par toutes les pages (jeton + profil). */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private static readonly storageKey = 'metamind.session';
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly user = signal<UserSession | null>(null);
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  constructor() {
    this.restore();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.login(request).pipe(tap((response) => this.store(response)));
  }

  logout(): void {
    this.user.set(null);
    this.api.setToken('');
    try { localStorage.removeItem(SessionService.storageKey); } catch { /* stockage indisponible */ }
    this.router.navigateByUrl('/');
  }

  updateUser(user: UserSession): void {
    this.user.set(user);
    this.persist({ user });
  }

  /**
   * Tous les utilisateurs arrivent sur leur tableau de bord. Un administrateur
   * y conserve le lien Administration dans la barre, au lieu d'atterrir dans
   * une zone sans navigation de travail.
   */
  homeUrl(): string {
    return '/espace';
  }

  private store(response: AuthResponse): void {
    this.api.setToken(response.token);
    this.user.set(response.user);
    this.persist(response);
  }

  private persist(partial: Partial<AuthResponse>): void {
    try {
      const current = JSON.parse(localStorage.getItem(SessionService.storageKey) ?? '{}');
      localStorage.setItem(SessionService.storageKey, JSON.stringify({ ...current, ...partial }));
    } catch { /* stockage indisponible : session en mémoire seulement */ }
  }

  private restore(): void {
    try {
      const stored = JSON.parse(localStorage.getItem(SessionService.storageKey) ?? 'null') as AuthResponse | null;
      if (stored?.token && stored.user) {
        this.api.setToken(stored.token);
        this.user.set(stored.user);
      }
    } catch {
      localStorage.removeItem(SessionService.storageKey);
    }
  }
}

import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  title: string;
  message: string;
  action: string;
  /** Une action destructrice s'affiche en rouge ; une validation, comme publier, en couleur principale. */
  tone?: 'danger' | 'primary';
}

interface PendingConfirm extends ConfirmRequest {
  resolve: (accepted: boolean) => void;
}

/** Confirmation des actions destructrices, affichée par la coquille dans le style de l'application. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly pending = signal<PendingConfirm | null>(null);

  ask(request: ConfirmRequest): Promise<boolean> {
    this.pending()?.resolve(false);
    return new Promise((resolve) => this.pending.set({ ...request, resolve }));
  }

  answer(accepted: boolean): void {
    const current = this.pending();
    if (!current) return;
    this.pending.set(null);
    current.resolve(accepted);
  }
}

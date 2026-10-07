import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { SessionService } from '../core/session.service';
import { ToastService } from '../core/toast.service';
import { ConfirmService } from '../core/confirm.service';

/** Profil et droits RGPD (accès, rectification, effacement). */
@Component({
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  template: `
    <div class="m-wrap m-wrap--narrow m-page m-stack">
      <h1 class="m-title">{{ 'Profil' | t }}</h1>
      <form class="m-sheet m-sheet--pad m-stack" (ngSubmit)="save()">
        <div class="m-grid2">
          <label class="m-field">{{ 'Prénom' | t }}<input class="m-input" name="firstName" [(ngModel)]="firstName" /></label>
          <label class="m-field">{{ 'Nom' | t }}<input class="m-input" name="lastName" [(ngModel)]="lastName" /></label>
        </div>
        <p class="m-small m-muted">{{ session.user()?.email }} · {{ session.user()?.institution }}</p>
        <div class="m-actions"><button type="submit" class="m-btn m-btn--primary">{{ 'Enregistrer' | t }}</button></div>
      </form>
      <section class="m-sheet m-sheet--pad m-stack">
        <h2 class="m-h3">{{ 'Mes données' | t }}</h2>
        <p class="m-muted">{{ 'Vous pouvez télécharger toutes les données vous concernant ou demander la suppression de votre compte.' | t }}</p>
        <div class="m-actions">
          <button type="button" class="m-btn m-btn--ghost" (click)="exportData()">{{ 'Télécharger mes données' | t }}</button>
          @if (!session.isAdmin()) { <button type="button" class="m-btn m-btn--danger" (click)="deleteAccount()">{{ 'Supprimer mon compte' | t }}</button> }
        </div>
      </section>
    </div>
  `
})
export class ProfilPage {
  readonly session = inject(SessionService);
  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly i18n = inject(I18nService);
  firstName = this.session.user()?.firstName ?? '';
  lastName = this.session.user()?.lastName ?? '';

  save(): void {
    const user = this.session.user();
    if (!user) return;
    this.api.updateProfile(user.id, { firstName: this.firstName.trim(), lastName: this.lastName.trim(), institution: user.institution }).subscribe({
      next: (u) => { this.session.updateUser(u); this.toasts.show(this.i18n.t('Profil enregistré.')); },
      error: () => this.toasts.show(this.i18n.t('La modification a échoué.'), 'error')
    });
  }

  exportData(): void {
    const user = this.session.user();
    if (!user) return;
    this.api.exportPersonalData(user.id).subscribe((data) => {
      const link = document.createElement('a');
      link.href = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
      link.download = 'mes-donnees-metamind.json';
      link.click();
    });
  }

  async deleteAccount(): Promise<void> {
    const user = this.session.user();
    if (!user) return;
    const accepted = await this.confirm.ask({
      title: this.i18n.t('Supprimer votre compte ?'),
      message: this.i18n.t('Vos données personnelles seront anonymisées et vous serez déconnecté. Les notices que vous avez validées restent publiées.'),
      action: this.i18n.t('Supprimer mon compte')
    });
    if (!accepted) return;
    this.api.requestAccountDeletion(user.id).subscribe({ next: () => this.session.logout(), error: () => this.toasts.show(this.i18n.t('La demande a échoué.'), 'error') });
  }
}

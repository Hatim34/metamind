import { Component, OnDestroy, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService, CreditAccount, CreditPackOption } from '../api.service';
import { I18nService, TranslatePipe } from '../core/i18n';
import { ToastService } from '../core/toast.service';

/** Crédits (B9, B10) : solde, achat via Stripe Checkout avec CGV et renonciation à la rétractation, historique. */
@Component({
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe, TranslatePipe],
  template: `
    <div class="m-wrap m-page">
      @if (returning()) {
        <div class="m-banner" role="status">
          @if (confirmedAfterReturn()) { {{ 'Paiement confirmé, vos crédits ont été ajoutés.' | t }} }
          @else { {{ 'Paiement en cours de confirmation…' | t }} }
        </div>
      }

      <div class="m-credits-top">
        <div class="m-balance">
          <span>{{ 'Solde de' | t }} {{ account()?.balance?.institution }}</span>
          <strong>{{ account()?.balance?.balance ?? '–' }} <small>{{ 'crédits' | t }}</small></strong>
          <span class="m-small">{{ '1 crédit = 1 extraction réussie.' | t }}</span>
        </div>
        <div class="m-packs">
          @for (pack of packs(); track pack.id) {
            <div class="m-pack" [class.is-on]="chosen()?.id === pack.id">
              <strong>{{ pack.credits }} {{ 'crédits' | t }}</strong>
              <span class="m-pack__price">{{ pack.amount | number: '1.0-2' }} {{ pack.currency.toUpperCase() === 'EUR' ? '€' : pack.currency }}</span>
              <span class="m-muted m-small">{{ (pack.amount / pack.credits) | number: '1.2-2' }} € {{ 'par extraction' | t }}</span>
              <button type="button" class="m-btn" [class.m-btn--primary]="chosen()?.id === pack.id" [class.m-btn--ghost]="chosen()?.id !== pack.id" (click)="choose(pack)">{{ 'Choisir' | t }}</button>
            </div>
          } @empty {
            <p class="m-muted">{{ 'Aucune offre disponible pour le moment.' | t }}</p>
          }
        </div>
      </div>

      @if (chosen(); as pack) {
        <form class="m-sheet m-sheet--pad m-stack" (ngSubmit)="pay(pack)">
          <strong>{{ pack.credits }} {{ 'crédits pour' | t }} {{ pack.amount | number: '1.0-2' }} €</strong>
          <label class="m-check"><input type="checkbox" name="cgv" [(ngModel)]="cgv" /> <span>{{ 'J\\'accepte les' | t }} <a routerLink="/legal" fragment="cgv">{{ 'conditions générales de vente' | t }}</a>.</span></label>
          <label class="m-check"><input type="checkbox" name="retractation" [(ngModel)]="waiver" /> <span>{{ 'Je demande l\\'accès immédiat aux crédits et renonce à mon droit de rétractation de 14 jours.' | t }}</span></label>
          <div class="m-actions">
            <button type="submit" class="m-btn m-btn--primary" [disabled]="!cgv || !waiver || busy()">{{ 'Payer avec Stripe' | t }}</button>
            <button type="button" class="m-btn m-btn--ghost" (click)="chosen.set(null)">{{ 'Annuler' | t }}</button>
          </div>
        </form>
      }

      <section class="m-sheet m-scroll">
        <div class="m-sheet__head"><h2 class="m-h3">{{ 'Historique' | t }}</h2></div>
        <table class="m-table">
          <thead><tr><th>{{ 'Date' | t }}</th><th>{{ 'Opération' | t }}</th><th class="m-right">{{ 'Crédits' | t }}</th><th class="m-right">{{ 'Solde' | t }}</th></tr></thead>
          <tbody>
            @for (mv of account()?.movements ?? []; track mv.id) {
              <tr>
                <td class="m-muted">{{ mv.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ mv.description }}</td>
                <td class="m-right m-num" [class.m-pos]="mv.amount > 0">{{ mv.amount > 0 ? '+' : '' }}{{ mv.amount }}</td>
                <td class="m-right m-num">{{ mv.balanceAfter }}</td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="m-empty">{{ 'Aucun mouvement.' | t }}</td></tr>
            }
          </tbody>
        </table>
      </section>
    </div>
  `
})
export class CreditsPage implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly toasts = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly account = signal<CreditAccount | null>(null);
  readonly packs = signal<CreditPackOption[]>([]);
  readonly chosen = signal<CreditPackOption | null>(null);
  readonly busy = signal(false);
  readonly returning = signal(this.route.snapshot.data['retourPaiement'] === true);
  readonly confirmedAfterReturn = signal(false);
  cgv = false;
  waiver = false;
  private timer?: ReturnType<typeof setInterval>;

  constructor() {
    this.loadAccount();
    // Les packs gratuits ne sont pas proposés : seuls les achats payés via Stripe ajoutent des crédits (prompt 1).
    this.api.getCreditPacks().subscribe({ next: (packs) => this.packs.set(packs.filter((p) => p.amount > 0)), error: () => undefined });
    if (this.returning()) {
      const start = this.account()?.balance.balance;
      let tries = 0;
      this.timer = setInterval(() => {
        tries++;
        this.api.getCreditAccount().subscribe((account) => {
          const before = start ?? this.account()?.balance.balance;
          this.account.set(account);
          if (before !== undefined && account.balance.balance > before) { this.confirmedAfterReturn.set(true); clearInterval(this.timer); }
        });
        if (tries >= 15) clearInterval(this.timer);
      }, 2000);
    }
  }

  loadAccount(): void {
    this.api.getCreditAccount().subscribe({ next: (a) => this.account.set(a), error: () => undefined });
  }

  choose(pack: CreditPackOption): void {
    this.chosen.set(pack);
    this.cgv = false;
    this.waiver = false;
  }

  pay(pack: CreditPackOption): void {
    if (!this.cgv || !this.waiver) return;
    this.busy.set(true);
    this.api.startCreditCheckout(pack.id, this.cgv, this.waiver).subscribe({
      next: (checkout) => window.location.assign(checkout.checkout_url),
      error: (e) => {
        this.busy.set(false);
        this.toasts.show(this.i18n.t(e?.status === 403 ? 'Les achats sont suspendus pour votre institution.' : 'Le paiement n\'a pas pu démarrer.'), 'error');
      }
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }
}

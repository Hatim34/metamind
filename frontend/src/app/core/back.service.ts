import { Injectable, inject } from '@angular/core';
import { Location } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

/**
 * Retour à la page précédente de l'application. Si la page a été ouverte directement
 * (lien partagé, favori, nouvel onglet), il n'y a pas de page précédente dans le site :
 * on va alors vers la page de repli indiquée.
 */
@Injectable({ providedIn: 'root' })
export class BackService {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private visited = 0;

  constructor() {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => this.visited++);
  }

  back(fallback: string): void {
    if (this.visited > 1) {
      this.location.back();
    } else {
      this.router.navigateByUrl(fallback);
    }
  }
}

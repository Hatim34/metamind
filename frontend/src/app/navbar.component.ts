import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

export type NavigationPage = 'accueil' | 'catalogue' | 'profil' | 'publication' | 'validation' | 'credits' | 'administration' | 'connexion' | 'inscription' | 'password-reset' | 'legal';
export type NavigationLanguage = 'fr' | 'nl' | 'en';

export interface NavigationLabels {
  title: string;
  catalogue: string;
  newPublication: string;
  validationQueue: string;
  credits: string;
  administration: string;
  profile: string;
  login: string;
  register: string;
  language: string;
  signOut: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  @Input({ required: true }) page: NavigationPage | 'detail' = 'catalogue';
  @Input() authenticated = false;
  @Input() administrator = false;
  @Input() userName = '';
  @Input() institutionName = '';
  @Input({ required: true }) language: NavigationLanguage = 'fr';
  @Input({ required: true }) labels!: NavigationLabels;

  @Output() pageChange = new EventEmitter<NavigationPage>();
  @Output() languageChange = new EventEmitter<NavigationLanguage>();
  @Output() signOut = new EventEmitter<void>();

  navigate(page: NavigationPage): void {
    this.pageChange.emit(page);
  }

  changeLanguage(language: NavigationLanguage): void {
    this.languageChange.emit(language);
  }

  get initials(): string {
    const parts = this.userName.trim().split(/\s+/).filter(Boolean);
    return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'M';
  }
}

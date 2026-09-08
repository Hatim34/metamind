import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

export type NavigationPage = 'catalogue' | 'profil' | 'publication' | 'administration' | 'connexion' | 'inscription' | 'password-reset';
export type NavigationLanguage = 'fr' | 'nl' | 'en';

export interface NavigationLabels {
  title: string;
  catalogue: string;
  newPublication: string;
  administration: string;
  profile: string;
  login: string;
  register: string;
  language: string;
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
  @Input({ required: true }) language: NavigationLanguage = 'fr';
  @Input({ required: true }) labels!: NavigationLabels;

  @Output() pageChange = new EventEmitter<NavigationPage>();
  @Output() languageChange = new EventEmitter<NavigationLanguage>();

  navigate(page: NavigationPage): void {
    this.pageChange.emit(page);
  }

  changeLanguage(language: NavigationLanguage): void {
    this.languageChange.emit(language);
  }
}

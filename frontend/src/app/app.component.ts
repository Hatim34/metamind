import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

type Page = 'catalogue' | 'connexion' | 'inscription' | 'profil';

interface Publication {
  title: string;
  author: string;
  institution: string;
  year: number;
  status: string;
  visibility: 'PUBLIC' | 'INSTITUTION';
  keywords: string[];
}

interface UserSession {
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  institution: string;
  status: 'ACTIF' | 'DESACTIVE';
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  page: Page = 'catalogue';
  search = '';
  deletionRequested = false;

  loginForm = {
    email: 'sarah@institution-a.example',
    password: ''
  };

  registerForm = {
    firstName: '',
    lastName: '',
    email: '',
    institution: ''
  };

  session: UserSession | null = null;

  publications: Publication[] = [
    {
      title: 'Analyse automatique des métadonnées pour les dépôts institutionnels',
      author: 'Sarah Lemaire',
      institution: 'Institution A',
      year: 2026,
      status: 'PUBLIE',
      visibility: 'PUBLIC',
      keywords: ['Dublin Core', 'métadonnées', 'recherche']
    },
    {
      title: 'Validation humaine des suggestions produites par un modèle de langage',
      author: 'Jan Peeters',
      institution: 'Institution B',
      year: 2025,
      status: 'A_VALIDER',
      visibility: 'INSTITUTION',
      keywords: ['validation', 'catalogage', 'qualité']
    },
    {
      title: 'Indexation multilingue de publications scientifiques',
      author: 'Mina Laurent',
      institution: 'Institution A',
      year: 2024,
      status: 'PUBLIE',
      visibility: 'PUBLIC',
      keywords: ['indexation', 'recherche', 'multilingue']
    }
  ];

  get filteredPublications(): Publication[] {
    const value = this.search.trim().toLowerCase();

    if (!value) {
      return this.publications;
    }

    return this.publications.filter((publication) =>
      [
        publication.title,
        publication.author,
        publication.institution,
        publication.status,
        publication.visibility,
        ...publication.keywords
      ]
        .join(' ')
        .toLowerCase()
        .includes(value)
    );
  }

  navigate(page: Page): void {
    this.page = page;
  }

  login(): void {
    this.session = {
      firstName: 'Sarah',
      lastName: 'Lemaire',
      email: this.loginForm.email || 'sarah@institution-a.example',
      role: 'Bibliothécaire',
      institution: 'Institution A',
      status: 'ACTIF'
    };
    this.deletionRequested = false;
    this.page = 'profil';
  }

  register(): void {
    this.session = {
      firstName: this.registerForm.firstName || 'Nouveau',
      lastName: this.registerForm.lastName || 'Bibliothécaire',
      email: this.registerForm.email || 'nouveau@institution-a.example',
      role: 'Bibliothécaire',
      institution: this.registerForm.institution || 'Institution A',
      status: 'ACTIF'
    };
    this.deletionRequested = false;
    this.page = 'profil';
  }

  requestDeletion(): void {
    if (!this.session) {
      return;
    }

    this.session = {
      ...this.session,
      status: 'DESACTIVE'
    };
    this.deletionRequested = true;
  }

  logout(): void {
    this.session = null;
    this.deletionRequested = false;
    this.page = 'catalogue';
  }
}

import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService, Institution, Publication, UserSession } from './api.service';

type Page = 'catalogue' | 'connexion' | 'inscription' | 'profil' | 'publication' | 'administration';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  page: Page = 'catalogue';
  search = '';
  deletionRequested = false;
  loading = false;
  message = '';
  profileSaved = false;

  loginForm = {
    email: 'sarah@institution-a.example',
    password: 'MotDePasse123'
  };

  adminLoginForm = {
    email: 'admin@metamind.example',
    password: 'MotDePasse123'
  };

  registerForm = {
    firstName: '',
    lastName: '',
    email: '',
    institution: '',
    password: ''
  };

  publicationForm = {
    title: '',
    author: '',
    year: new Date().getFullYear(),
    visibility: 'PUBLIC' as 'PUBLIC' | 'INSTITUTION',
    keywords: ''
  };

  profileForm = {
    firstName: '',
    lastName: '',
    institution: ''
  };

  institutionForm = {
    code: '',
    name: '',
    emailDomain: ''
  };

  session: UserSession | null = null;
  token = '';
  publications: Publication[] = [];
  institutions: Institution[] = [];

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadPublications();
  }

  navigate(page: Page): void {
    this.page = page;
    if (page === 'administration') {
      this.loadInstitutions();
    }
  }

  loadPublications(): void {
    this.loading = true;
    this.api.getPublications(this.search).subscribe({
      next: (publications) => {
        this.publications = publications;
        this.loading = false;
        this.message = '';
      },
      error: () => {
        this.loading = false;
        this.message = "Impossible de joindre l'API locale.";
      }
    });
  }

  login(): void {
    this.api.login(this.loginForm).subscribe({
      next: (response) => {
        this.token = response.token;
        this.session = response.user;
        this.fillProfileForm(response.user);
        this.deletionRequested = false;
        this.profileSaved = false;
        this.message = '';
        this.page = 'profil';
        if (this.isAdmin) {
          this.loadInstitutions();
        }
      },
      error: () => {
        this.message = 'Connexion impossible avec les donnees envoyees.';
      }
    });
  }

  register(): void {
    this.api.register(this.registerForm).subscribe({
      next: (response) => {
        this.token = response.token;
        this.session = response.user;
        this.fillProfileForm(response.user);
        this.deletionRequested = false;
        this.profileSaved = false;
        this.message = '';
        this.page = 'profil';
      },
      error: () => {
        this.message = "Creation du compte impossible avec les donnees envoyees.";
      }
    });
  }

  useAdminAccount(): void {
    this.loginForm = { ...this.adminLoginForm };
  }

  useLibrarianAccount(): void {
    this.loginForm = {
      email: 'sarah@institution-a.example',
      password: 'MotDePasse123'
    };
  }

  createPublication(): void {
    if (!this.session) {
      this.message = 'Connectez-vous pour ajouter une publication.';
      return;
    }

    this.api.createPublication({
      title: this.publicationForm.title,
      author: this.publicationForm.author,
      institution: this.session.institution,
      year: this.publicationForm.year,
      visibility: this.publicationForm.visibility,
      keywords: this.publicationForm.keywords.split(',').map((keyword) => keyword.trim()).filter(Boolean)
    }).subscribe({
      next: () => {
        this.publicationForm = {
          title: '',
          author: '',
          year: new Date().getFullYear(),
          visibility: 'PUBLIC',
          keywords: ''
        };
        this.page = 'catalogue';
        this.loadPublications();
      },
      error: () => {
        this.message = "Creation de la publication impossible avec les donnees envoyees.";
      }
    });
  }

  updateProfile(): void {
    if (!this.session) {
      return;
    }

    this.api.updateProfile(this.session.id, this.profileForm).subscribe({
      next: (user) => {
        this.session = user;
        this.fillProfileForm(user);
        this.profileSaved = true;
        this.message = '';
      },
      error: () => {
        this.message = 'Modification du profil impossible.';
      }
    });
  }

  requestDeletion(): void {
    if (!this.session) {
      return;
    }

    this.api.requestAccountDeletion(this.session.id).subscribe({
      next: (user) => {
        this.session = user;
        this.deletionRequested = true;
        this.message = '';
      },
      error: () => {
        this.message = 'Demande de suppression impossible.';
      }
    });
  }

  loadInstitutions(): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = institutions;
        this.message = '';
      },
      error: () => {
        this.message = 'Chargement des institutions impossible.';
      }
    });
  }

  createInstitution(): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.createInstitution(this.institutionForm).subscribe({
      next: () => {
        this.institutionForm = { code: '', name: '', emailDomain: '' };
        this.loadInstitutions();
      },
      error: () => {
        this.message = "Creation de l'institution impossible.";
      }
    });
  }

  deactivateInstitution(institutionId: number): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.deactivateInstitution(institutionId).subscribe({
      next: () => this.loadInstitutions(),
      error: () => {
        this.message = "Desactivation de l'institution impossible.";
      }
    });
  }

  logout(): void {
    this.session = null;
    this.token = '';
    this.institutions = [];
    this.deletionRequested = false;
    this.profileSaved = false;
    this.page = 'catalogue';
  }

  get isAdmin(): boolean {
    return this.session?.role === 'Administrateur';
  }

  private fillProfileForm(user: UserSession): void {
    this.profileForm = {
      firstName: user.firstName,
      lastName: user.lastName,
      institution: user.institution
    };
  }
}

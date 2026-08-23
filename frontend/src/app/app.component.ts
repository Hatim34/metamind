import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService, Publication, UserSession } from './api.service';

type Page = 'catalogue' | 'connexion' | 'inscription' | 'profil' | 'publication';

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

  session: UserSession | null = null;
  token = '';
  publications: Publication[] = [];

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.loadPublications();
  }

  navigate(page: Page): void {
    this.page = page;
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

  logout(): void {
    this.session = null;
    this.token = '';
    this.deletionRequested = false;
    this.profileSaved = false;
    this.page = 'catalogue';
  }

  private fillProfileForm(user: UserSession): void {
    this.profileForm = {
      firstName: user.firstName,
      lastName: user.lastName,
      institution: user.institution
    };
  }
}

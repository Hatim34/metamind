import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService, Institution, MetadataExtraction, Publication, PublicationStatus, UserSession } from './api.service';

type Page = 'catalogue' | 'connexion' | 'inscription' | 'profil' | 'publication' | 'administration';
type Language = 'fr' | 'nl';

const translations = {
  fr: {
    title: 'Gestion des métadonnées académiques',
    catalogue: 'Catalogue',
    login: 'Connexion',
    register: 'Inscription',
    profile: 'Profil',
    newPublication: 'Nouvelle publication',
    administration: 'Administration',
    indicators: 'Indicateurs',
    publications: 'Publications',
    activeAccount: 'Compte actif',
    yes: 'Oui',
    no: 'Non',
    status: 'Statut',
    publicStatus: 'Public',
    credits: 'Crédits',
    availablePublications: 'Publications disponibles',
    searchPlaceholder: 'Rechercher un titre, auteur ou mot-clé',
    loadingCatalogue: 'Chargement du catalogue...',
    addPublication: 'Ajouter une publication',
    titleLabel: 'Titre',
    author: 'Auteur',
    year: 'Année',
    visibility: 'Visibilité',
    publicVisibility: 'Public',
    institutionOnly: 'Institution seulement',
    keywords: 'Mots-clés',
    addToCatalogue: 'Ajouter au catalogue',
    loginRequiredPublication: 'Connectez-vous pour ajouter une publication.',
    librarianSpace: "Accéder à l'espace bibliothécaire",
    email: 'Email',
    password: 'Mot de passe',
    signIn: 'Se connecter',
    librarianAccount: 'Compte bibliothécaire',
    adminAccount: 'Compte administrateur',
    createLibrarianAccount: 'Créer un compte bibliothécaire',
    firstName: 'Prénom',
    lastName: 'Nom',
    institution: 'Institution',
    createAccount: 'Créer le compte',
    userAccount: 'Compte utilisateur',
    role: 'Rôle',
    buyCredits: 'Acheter 10 crédits',
    requestDeletion: 'Demander la suppression du compte',
    logout: 'Se déconnecter',
    saveProfile: 'Enregistrer le profil',
    deletionRecorded: 'La demande est enregistrée. Le compte passe au statut DESACTIVE et sera traité selon les règles RGPD.',
    profileUpdated: 'Le profil est mis à jour.',
    loginRequiredProfile: 'Connectez-vous pour consulter votre profil.',
    partnerInstitutions: 'Institutions partenaires',
    code: 'Code',
    name: 'Nom',
    domain: 'Domaine email',
    action: 'Action',
    active: 'Active',
    inactive: 'Inactive',
    add: 'Ajouter',
    deactivate: 'Désactiver',
    adminRequired: 'Connectez-vous avec un compte administrateur.',
    extract: 'Extraire',
    publish: 'Publier',
    deletePublication: 'Supprimer',
    apiUnavailable: "Impossible de joindre l'API locale.",
    loginFailed: 'Connexion impossible avec les donnees envoyees.',
    registerFailed: 'Creation du compte impossible avec les donnees envoyees.',
    createPublicationFailed: 'Creation de la publication impossible avec les donnees envoyees.',
    purchaseFailed: 'Achat de credits impossible.',
    extractionFailed: 'Extraction impossible. Verifiez le solde de credits.',
    publicationPublished: 'La publication est publiee.',
    publicationDeleted: 'La publication est supprimee logiquement.',
    statusUpdateFailed: 'Modification du statut impossible.',
    updateProfileFailed: 'Modification du profil impossible.',
    deletionFailed: 'Demande de suppression impossible.',
    loadInstitutionsFailed: 'Chargement des institutions impossible.',
    createInstitutionFailed: "Creation de l'institution impossible.",
    deactivateInstitutionFailed: "Desactivation de l'institution impossible.",
    extractedMetadataPrefix: 'Métadonnées extraites pour',
    language: 'Langue',
    invalidForm: 'Veuillez compléter correctement les champs obligatoires.'
  },
  nl: {
    title: 'Beheer van academische metadata',
    catalogue: 'Catalogus',
    login: 'Aanmelden',
    register: 'Registreren',
    profile: 'Profiel',
    newPublication: 'Nieuwe publicatie',
    administration: 'Beheer',
    indicators: 'Indicatoren',
    publications: 'Publicaties',
    activeAccount: 'Actieve account',
    yes: 'Ja',
    no: 'Nee',
    status: 'Status',
    publicStatus: 'Publiek',
    credits: 'Credits',
    availablePublications: 'Beschikbare publicaties',
    searchPlaceholder: 'Zoek op titel, auteur of trefwoord',
    loadingCatalogue: 'Catalogus wordt geladen...',
    addPublication: 'Een publicatie toevoegen',
    titleLabel: 'Titel',
    author: 'Auteur',
    year: 'Jaar',
    visibility: 'Zichtbaarheid',
    publicVisibility: 'Publiek',
    institutionOnly: 'Alleen instelling',
    keywords: 'Trefwoorden',
    addToCatalogue: 'Toevoegen aan catalogus',
    loginRequiredPublication: 'Meld u aan om een publicatie toe te voegen.',
    librarianSpace: 'Toegang tot de bibliothecarisruimte',
    email: 'E-mail',
    password: 'Wachtwoord',
    signIn: 'Aanmelden',
    librarianAccount: 'Bibliothecarisaccount',
    adminAccount: 'Beheerdersaccount',
    createLibrarianAccount: 'Een bibliothecarisaccount maken',
    firstName: 'Voornaam',
    lastName: 'Naam',
    institution: 'Instelling',
    createAccount: 'Account maken',
    userAccount: 'Gebruikersaccount',
    role: 'Rol',
    buyCredits: '10 credits kopen',
    requestDeletion: 'Verwijdering van de account aanvragen',
    logout: 'Afmelden',
    saveProfile: 'Profiel opslaan',
    deletionRecorded: 'De aanvraag is geregistreerd. De account krijgt de status DESACTIVE en wordt volgens de AVG-regels behandeld.',
    profileUpdated: 'Het profiel is bijgewerkt.',
    loginRequiredProfile: 'Meld u aan om uw profiel te bekijken.',
    partnerInstitutions: 'Partnerinstellingen',
    code: 'Code',
    name: 'Naam',
    domain: 'E-maildomein',
    action: 'Actie',
    active: 'Actief',
    inactive: 'Inactief',
    add: 'Toevoegen',
    deactivate: 'Deactiveren',
    adminRequired: 'Meld u aan met een beheerdersaccount.',
    extract: 'Extraheren',
    publish: 'Publiceren',
    deletePublication: 'Verwijderen',
    apiUnavailable: 'De lokale API is niet bereikbaar.',
    loginFailed: 'Aanmelden is onmogelijk met de verzonden gegevens.',
    registerFailed: 'Account aanmaken is onmogelijk met de verzonden gegevens.',
    createPublicationFailed: 'Publicatie aanmaken is onmogelijk met de verzonden gegevens.',
    purchaseFailed: 'Credits kopen is onmogelijk.',
    extractionFailed: 'Extractie is onmogelijk. Controleer het creditsaldo.',
    publicationPublished: 'De publicatie is gepubliceerd.',
    publicationDeleted: 'De publicatie is logisch verwijderd.',
    statusUpdateFailed: 'Status wijzigen is onmogelijk.',
    updateProfileFailed: 'Profiel wijzigen is onmogelijk.',
    deletionFailed: 'Verwijderingsaanvraag is onmogelijk.',
    loadInstitutionsFailed: 'Instellingen laden is onmogelijk.',
    createInstitutionFailed: 'Instelling aanmaken is onmogelijk.',
    deactivateInstitutionFailed: 'Instelling deactiveren is onmogelijk.',
    extractedMetadataPrefix: 'Metadata geextraheerd voor',
    language: 'Taal',
    invalidForm: 'Vul de verplichte velden correct in.'
  }
} as const;

type TranslationKey = keyof typeof translations.fr;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  page: Page = 'catalogue';
  language: Language = 'fr';
  search = '';
  deletionRequested = false;
  loading = false;
  message = '';
  profileSaved = false;

  loginForm = {
    email: 'sarah@institution-a.example',
    password: '558435'
  };

  adminLoginForm = {
    email: 'admin@metamind.example',
    password: '558435'
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
  creditBalance: number | null = null;
  extractionResult: MetadataExtraction | null = null;

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

  setLanguage(language: Language): void {
    this.language = language;
  }

  t(key: TranslationKey): string {
    return translations[this.language][key];
  }

  loadPublications(clearMessage = true): void {
    this.loading = true;
    this.api.getPublications(this.search).subscribe({
      next: (publications) => {
        this.publications = publications;
        this.loading = false;
        if (clearMessage) {
          this.message = '';
        }
      },
      error: () => {
        this.loading = false;
        this.message = this.t('apiUnavailable');
      }
    });
  }

  login(): void {
    this.api.login(this.loginForm).subscribe({
      next: (response) => {
        this.token = response.token;
        this.api.setToken(response.token);
        this.session = response.user;
        this.fillProfileForm(response.user);
        this.deletionRequested = false;
        this.profileSaved = false;
        this.message = '';
        this.page = 'profil';
        this.loadCredits();
        if (this.isAdmin) {
          this.loadInstitutions();
        }
      },
      error: () => {
        this.message = this.t('loginFailed');
      }
    });
  }

  register(): void {
    if (!this.isRegisterFormValid()) {
      this.message = this.t('invalidForm');
      return;
    }

    this.api.register(this.registerForm).subscribe({
      next: (response) => {
        this.token = response.token;
        this.api.setToken(response.token);
        this.session = response.user;
        this.fillProfileForm(response.user);
        this.deletionRequested = false;
        this.profileSaved = false;
        this.message = '';
        this.page = 'profil';
        this.loadCredits();
      },
      error: () => {
        this.message = this.t('registerFailed');
      }
    });
  }

  useAdminAccount(): void {
    this.loginForm = { ...this.adminLoginForm };
  }

  useLibrarianAccount(): void {
    this.loginForm = {
      email: 'sarah@institution-a.example',
      password: '558435'
    };
  }

  createPublication(): void {
    if (!this.session) {
      this.message = this.t('loginRequiredPublication');
      return;
    }
    if (!this.isPublicationFormValid()) {
      this.message = this.t('invalidForm');
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
        this.message = this.t('createPublicationFailed');
      }
    });
  }

  loadCredits(): void {
    if (!this.session) {
      this.creditBalance = null;
      return;
    }

    this.api.getCreditBalance(this.session.id).subscribe({
      next: (credits) => {
        this.creditBalance = credits.balance;
      },
      error: () => {
        this.creditBalance = null;
      }
    });
  }

  purchaseCredits(amount: number): void {
    if (!this.session) {
      return;
    }

    this.api.purchaseCredits(this.session.id, amount).subscribe({
      next: (credits) => {
        this.creditBalance = credits.balance;
        this.message = '';
      },
      error: () => {
        this.message = this.t('purchaseFailed');
      }
    });
  }

  extractMetadata(publication: Publication): void {
    if (!this.session) {
      this.message = this.t('loginRequiredPublication');
      return;
    }

    this.api.extractMetadata(publication.id).subscribe({
      next: (result) => {
        this.extractionResult = result;
        this.creditBalance = result.creditBalance;
        this.message = '';
        this.loadPublications();
      },
      error: () => {
        this.message = this.t('extractionFailed');
      }
    });
  }

  updatePublicationStatus(publication: Publication, status: Extract<PublicationStatus, 'A_VALIDER' | 'PUBLIE' | 'SUPPRIME'>): void {
    if (!this.canManagePublication(publication)) {
      this.message = this.t('statusUpdateFailed');
      return;
    }

    this.api.updatePublicationStatus(publication.id, { status }).subscribe({
      next: (updatedPublication) => {
        this.publications = this.publications.map((item) => item.id === updatedPublication.id ? updatedPublication : item);
        this.message = status === 'SUPPRIME' ? this.t('publicationDeleted') : this.t('publicationPublished');
        this.loadPublications(false);
      },
      error: () => {
        this.message = this.t('statusUpdateFailed');
      }
    });
  }

  updateProfile(): void {
    if (!this.session) {
      return;
    }
    if (!this.profileForm.firstName.trim() || !this.profileForm.lastName.trim() || !this.profileForm.institution.trim()) {
      this.message = this.t('invalidForm');
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
        this.message = this.t('updateProfileFailed');
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
        this.message = this.t('deletionFailed');
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
        this.message = this.t('loadInstitutionsFailed');
      }
    });
  }

  createInstitution(): void {
    if (!this.isAdmin) {
      return;
    }
    if (!this.isInstitutionFormValid()) {
      this.message = this.t('invalidForm');
      return;
    }

    this.api.createInstitution(this.institutionForm).subscribe({
      next: () => {
        this.institutionForm = { code: '', name: '', emailDomain: '' };
        this.loadInstitutions();
      },
      error: () => {
        this.message = this.t('createInstitutionFailed');
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
        this.message = this.t('deactivateInstitutionFailed');
      }
    });
  }

  logout(): void {
    this.session = null;
    this.token = '';
    this.api.setToken('');
    this.institutions = [];
    this.creditBalance = null;
    this.extractionResult = null;
    this.deletionRequested = false;
    this.profileSaved = false;
    this.page = 'catalogue';
  }

  get isAdmin(): boolean {
    return this.session?.role === 'Administrateur';
  }

  canManagePublication(publication: Publication): boolean {
    return !!this.session && (this.isAdmin || publication.institution === this.session.institution);
  }

  canExtractPublication(publication: Publication): boolean {
    return !!this.session && publication.institution === this.session.institution && publication.status !== 'SUPPRIME';
  }

  canPublishPublication(publication: Publication): boolean {
    return this.canManagePublication(publication) && publication.status !== 'PUBLIE' && publication.status !== 'SUPPRIME';
  }

  canDeletePublication(publication: Publication): boolean {
    return this.canManagePublication(publication) && publication.status !== 'SUPPRIME';
  }

  isPublicationFormValid(): boolean {
    return this.publicationForm.title.trim().length >= 3
      && this.publicationForm.author.trim().length >= 2
      && this.publicationForm.year >= 1900
      && this.publicationForm.year <= 2100;
  }

  isRegisterFormValid(): boolean {
    return this.registerForm.firstName.trim().length >= 2
      && this.registerForm.lastName.trim().length >= 2
      && this.registerForm.email.includes('@')
      && this.registerForm.institution.trim().length >= 2
      && this.registerForm.password.length >= 8;
  }

  isInstitutionFormValid(): boolean {
    return this.institutionForm.code.trim().length >= 3
      && this.institutionForm.name.trim().length >= 2
      && this.institutionForm.emailDomain.includes('.');
  }

  private fillProfileForm(user: UserSession): void {
    this.profileForm = {
      firstName: user.firstName,
      lastName: user.lastName,
      institution: user.institution
    };
  }
}

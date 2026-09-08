import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService, AuditLog, AuthResponse, CreditMovement, CreditPackOption, DashboardStatistics, Institution, MetadataDetails, MetadataExtraction, Publication, PublicationStatus, SearchFilters, UserSession } from './api.service';
import { NavigationLabels, NavbarComponent } from './navbar.component';
import { PublicationCardLabels, PublicationCardComponent } from './publication-card.component';
import { PublicationDetailComponent, PublicationDetailLabels } from './publication-detail.component';

type Page = 'catalogue' | 'detail' | 'connexion' | 'inscription' | 'profil' | 'publication' | 'administration' | 'password-reset';
type Language = 'fr' | 'nl' | 'en';

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
    published: 'Publiées',
    pendingValidation: 'À valider',
    activeAccount: 'Compte actif',
    yes: 'Oui',
    no: 'Non',
    status: 'Statut',
    processing: 'Traitement en cours',
    publicStatus: 'Public',
    credits: 'Crédits',
    creditHistory: 'Historique des crédits',
    movementDate: 'Date',
    movementType: 'Type',
    movementAmount: 'Mouvement',
    movementBalance: 'Solde après',
    movementDescription: 'Description',
    noCreditMovement: 'Aucun mouvement de crédit enregistré.',
    availablePublications: 'Publications disponibles',
    loadingCatalogue: 'Chargement du catalogue...',
    addPublication: 'Ajouter une publication',
    consult: 'Consulter',
    filters: 'Filtres',
    clearFilters: 'Effacer les filtres',
    startDate: 'Date minimale',
    endDate: 'Date maximale',
    publicationDetails: 'Fiche publication',
    backToCatalogue: 'Retour au catalogue',
    downloadFile: 'Consulter le fichier',
    noFile: 'Fichier non disponible.',
    noSummary: 'Aucun résumé disponible.',
    documentType: 'Type de document',
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
    forgotPassword: 'Mot de passe oublie',
    resetPassword: 'Reinitialiser le mot de passe',
    resetEmail: 'Adresse e-mail',
    sendResetLink: 'Envoyer le lien',
    resetToken: 'Code de reinitialisation',
    newPassword: 'Nouveau mot de passe',
    confirmPasswordReset: 'Modifier le mot de passe',
    resetRequestSent: 'Si le compte existe, un lien de reinitialisation a ete envoye.',
    passwordResetDone: 'Mot de passe modifie. Vous pouvez vous connecter.',
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
    availablePacks: 'Packs disponibles',
    checkoutStarted: 'Paiement confirme, le solde est mis a jour.',
    importFile: 'Importer un document',
    selectedFile: 'Fichier sélectionné',
    coverImage: 'Image de couverture',
    sendFile: 'Importer le fichier',
    importQueued: 'Import en cours de traitement.',
    adminUsers: 'Utilisateurs',
    configuration: 'Configuration',
    saveConfiguration: 'Enregistrer la configuration',
    configurationSaved: 'La configuration est mise à jour.',
    configurationFailed: 'Modification de la configuration impossible.',
    exportCsv: 'Exporter le CSV',
    exportFailed: 'Export CSV impossible.',
    auditLogs: 'Journal d audit',
    deactivateAccount: 'Désactiver',
    requestDeletion: 'Demander la suppression du compte',
    logout: 'Se déconnecter',
    saveProfile: 'Enregistrer le profil',
    deletionRecorded: 'La demande est enregistrée. Le compte passe au statut DESACTIVE et les données personnelles sont anonymisées.',
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
    validateMetadataAction: 'Valider les métadonnées',
    editMetadata: 'Corriger les métadonnées',
    summary: 'Résumé',
    extractedText: 'Texte extrait',
    noExtractedText: 'Aucun texte extrait disponible.',
    publicationDate: 'Date de publication',
    classification: 'Classification',
    cancel: 'Annuler',
    deletePublication: 'Supprimer',
    apiUnavailable: "Impossible de joindre l'API locale.",
    loginFailed: 'Connexion impossible avec les donnees envoyees.',
    registerFailed: 'Creation du compte impossible avec les donnees envoyees.',
    createPublicationFailed: 'Creation de la publication impossible avec les donnees envoyees.',
    purchaseFailed: 'Achat de credits impossible.',
    extractionFailed: 'Extraction impossible. Verifiez le solde de credits.',
    publicationPublished: 'La publication est publiee.',
    metadataLoaded: 'Les métadonnées sont prêtes à être corrigées.',
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
    published: 'Gepubliceerd',
    pendingValidation: 'Te valideren',
    activeAccount: 'Actieve account',
    yes: 'Ja',
    no: 'Nee',
    status: 'Status',
    processing: 'Verwerking bezig',
    publicStatus: 'Publiek',
    credits: 'Credits',
    creditHistory: 'Creditgeschiedenis',
    movementDate: 'Datum',
    movementType: 'Type',
    movementAmount: 'Beweging',
    movementBalance: 'Saldo na verwerking',
    movementDescription: 'Beschrijving',
    noCreditMovement: 'Geen creditbeweging geregistreerd.',
    availablePublications: 'Beschikbare publicaties',
    loadingCatalogue: 'Catalogus wordt geladen...',
    addPublication: 'Een publicatie toevoegen',
    consult: 'Bekijken',
    filters: 'Filters',
    clearFilters: 'Filters wissen',
    startDate: 'Begindatum',
    endDate: 'Einddatum',
    publicationDetails: 'Publicatiefiche',
    backToCatalogue: 'Terug naar catalogus',
    downloadFile: 'Bestand bekijken',
    noFile: 'Bestand niet beschikbaar.',
    noSummary: 'Geen samenvatting beschikbaar.',
    documentType: 'Documenttype',
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
    forgotPassword: 'Wachtwoord vergeten',
    resetPassword: 'Wachtwoord opnieuw instellen',
    resetEmail: 'E-mailadres',
    sendResetLink: 'Link versturen',
    resetToken: 'Herstelcode',
    newPassword: 'Nieuw wachtwoord',
    confirmPasswordReset: 'Wachtwoord wijzigen',
    resetRequestSent: 'Als het account bestaat, is een herstelbericht verzonden.',
    passwordResetDone: 'Wachtwoord gewijzigd. U kunt zich aanmelden.',
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
    availablePacks: 'Beschikbare pakketten',
    checkoutStarted: 'Betaling bevestigd, het saldo is bijgewerkt.',
    importFile: 'Document importeren',
    selectedFile: 'Geselecteerd bestand',
    coverImage: 'Omslagafbeelding',
    sendFile: 'Bestand importeren',
    importQueued: 'De import wordt verwerkt.',
    adminUsers: 'Gebruikers',
    configuration: 'Configuratie',
    saveConfiguration: 'Configuratie opslaan',
    configurationSaved: 'De configuratie is bijgewerkt.',
    configurationFailed: 'Configuratie wijzigen is onmogelijk.',
    exportCsv: 'CSV exporteren',
    exportFailed: 'CSV-export is onmogelijk.',
    auditLogs: 'Auditlogboek',
    deactivateAccount: 'Deactiveren',
    requestDeletion: 'Verwijdering van de account aanvragen',
    logout: 'Afmelden',
    saveProfile: 'Profiel opslaan',
    deletionRecorded: 'De aanvraag is geregistreerd. De account krijgt de status DESACTIVE en de persoonsgegevens worden geanonimiseerd.',
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
    validateMetadataAction: 'Metadata valideren',
    editMetadata: 'Metadata corrigeren',
    summary: 'Samenvatting',
    extractedText: 'Geextraheerde tekst',
    noExtractedText: 'Geen geextraheerde tekst beschikbaar.',
    publicationDate: 'Publicatiedatum',
    classification: 'Classificatie',
    cancel: 'Annuleren',
    deletePublication: 'Verwijderen',
    apiUnavailable: 'De lokale API is niet bereikbaar.',
    loginFailed: 'Aanmelden is onmogelijk met de verzonden gegevens.',
    registerFailed: 'Account aanmaken is onmogelijk met de verzonden gegevens.',
    createPublicationFailed: 'Publicatie aanmaken is onmogelijk met de verzonden gegevens.',
    purchaseFailed: 'Credits kopen is onmogelijk.',
    extractionFailed: 'Extractie is onmogelijk. Controleer het creditsaldo.',
    publicationPublished: 'De publicatie is gepubliceerd.',
    metadataLoaded: 'De metadata is klaar om gecorrigeerd te worden.',
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
  },
  en: {
    title: 'Academic metadata management',
    catalogue: 'Catalogue',
    login: 'Sign in',
    register: 'Register',
    profile: 'Profile',
    newPublication: 'New publication',
    administration: 'Administration',
    indicators: 'Indicators',
    publications: 'Publications',
    published: 'Published',
    pendingValidation: 'To validate',
    activeAccount: 'Active account',
    yes: 'Yes',
    no: 'No',
    status: 'Status',
    processing: 'Processing',
    publicStatus: 'Public',
    credits: 'Credits',
    creditHistory: 'Credit history',
    movementDate: 'Date',
    movementType: 'Type',
    movementAmount: 'Movement',
    movementBalance: 'Balance after',
    movementDescription: 'Description',
    noCreditMovement: 'No credit movement recorded.',
    availablePublications: 'Available publications',
    loadingCatalogue: 'Loading catalogue...',
    addPublication: 'Add a publication',
    consult: 'View',
    filters: 'Filters',
    clearFilters: 'Clear filters',
    startDate: 'Start date',
    endDate: 'End date',
    publicationDetails: 'Publication record',
    backToCatalogue: 'Back to catalogue',
    downloadFile: 'View file',
    noFile: 'File not available.',
    noSummary: 'No summary available.',
    documentType: 'Document type',
    titleLabel: 'Title',
    author: 'Author',
    year: 'Year',
    visibility: 'Visibility',
    publicVisibility: 'Public',
    institutionOnly: 'Institution only',
    keywords: 'Keywords',
    addToCatalogue: 'Add to catalogue',
    loginRequiredPublication: 'Sign in to add a publication.',
    librarianSpace: 'Access the librarian area',
    email: 'Email',
    password: 'Password',
    signIn: 'Sign in',
    forgotPassword: 'Forgot password',
    resetPassword: 'Reset password',
    resetEmail: 'Email address',
    sendResetLink: 'Send link',
    resetToken: 'Reset token',
    newPassword: 'New password',
    confirmPasswordReset: 'Change password',
    resetRequestSent: 'If the account exists, a reset link was sent.',
    passwordResetDone: 'Password changed. You can sign in.',
    librarianAccount: 'Librarian account',
    adminAccount: 'Administrator account',
    createLibrarianAccount: 'Create a librarian account',
    firstName: 'First name',
    lastName: 'Last name',
    institution: 'Institution',
    createAccount: 'Create account',
    userAccount: 'User account',
    role: 'Role',
    buyCredits: 'Buy 10 credits',
    availablePacks: 'Available packs',
    checkoutStarted: 'Payment confirmed, the balance is updated.',
    importFile: 'Import a document',
    selectedFile: 'Selected file',
    coverImage: 'Cover image',
    sendFile: 'Import file',
    importQueued: 'The import is being processed.',
    adminUsers: 'Users',
    configuration: 'Configuration',
    saveConfiguration: 'Save configuration',
    configurationSaved: 'The configuration is updated.',
    configurationFailed: 'Configuration update failed.',
    exportCsv: 'Export CSV',
    exportFailed: 'CSV export failed.',
    auditLogs: 'Audit log',
    deactivateAccount: 'Deactivate',
    requestDeletion: 'Request account deletion',
    logout: 'Sign out',
    saveProfile: 'Save profile',
    deletionRecorded: 'The request is recorded. The account moves to DESACTIVE status and personal data is anonymized.',
    profileUpdated: 'The profile is updated.',
    loginRequiredProfile: 'Sign in to view your profile.',
    partnerInstitutions: 'Partner institutions',
    code: 'Code',
    name: 'Name',
    domain: 'Email domain',
    action: 'Action',
    active: 'Active',
    inactive: 'Inactive',
    add: 'Add',
    deactivate: 'Deactivate',
    adminRequired: 'Sign in with an administrator account.',
    extract: 'Extract',
    publish: 'Publish',
    validateMetadataAction: 'Validate metadata',
    editMetadata: 'Edit metadata',
    summary: 'Summary',
    extractedText: 'Extracted text',
    noExtractedText: 'No extracted text available.',
    publicationDate: 'Publication date',
    classification: 'Classification',
    cancel: 'Cancel',
    deletePublication: 'Delete',
    apiUnavailable: 'Unable to reach the local API.',
    loginFailed: 'Sign-in failed with the submitted data.',
    registerFailed: 'Account creation failed with the submitted data.',
    createPublicationFailed: 'Publication creation failed with the submitted data.',
    purchaseFailed: 'Credit purchase failed.',
    extractionFailed: 'Extraction failed. Check the credit balance.',
    publicationPublished: 'The publication is published.',
    metadataLoaded: 'The metadata is ready for review.',
    publicationDeleted: 'The publication is logically deleted.',
    statusUpdateFailed: 'Status update failed.',
    updateProfileFailed: 'Profile update failed.',
    deletionFailed: 'Deletion request failed.',
    loadInstitutionsFailed: 'Institution loading failed.',
    createInstitutionFailed: 'Institution creation failed.',
    deactivateInstitutionFailed: 'Institution deactivation failed.',
    extractedMetadataPrefix: 'Metadata extracted for',
    language: 'Language',
    invalidForm: 'Please complete the required fields correctly.'
  }
} as const;

type TranslationKey = keyof typeof translations.fr;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, PublicationCardComponent, PublicationDetailComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private static readonly sessionStorageKey = 'metamind.session';
  page: Page = 'catalogue';
  language: Language = 'fr';
  search = '';
  deletionRequested = false;
  loading = false;
  message = '';
  profileSaved = false;
  searchFilters: SearchFilters = {
    author: '',
    language: '',
    documentType: '',
    startDate: '',
    endDate: ''
  };

  loginForm = {
    email: '',
    password: ''
  };

  passwordResetForm = {
    email: '',
    token: '',
    password: ''
  };
  passwordResetRequested = false;

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
    keywords: '',
    image: null as File | null
  };

  importForm = {
    file: null as File | null,
    image: null as File | null,
    visibility: 'INSTITUTION' as 'PUBLIC' | 'INSTITUTION'
  };

  metadataForm = {
    documentId: 0,
    title: '',
    summary: '',
    publicationDate: '',
    classification: '',
    visibility: 'PUBLIC' as 'PUBLIC' | 'INSTITUTION',
    authors: '',
    keywords: '',
    extractedText: ''
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
  selectedPublication: Publication | null = null;
  institutions: Institution[] = [];
  adminUsers: UserSession[] = [];
  adminConfig: Record<string, string> = {};
  adminConfigForm: Record<string, string> = {};
  auditLogs: AuditLog[] = [];
  creditPacks: CreditPackOption[] = [];
  creditBalance: number | null = null;
  creditMovements: CreditMovement[] = [];
  statistics: DashboardStatistics | null = null;
  extractionResult: MetadataExtraction | null = null;
  selectedMetadataPublicationId: number | null = null;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.restoreSession();
    const resetToken = new URLSearchParams(window.location.search).get('token');
    if (resetToken) {
      this.passwordResetForm.token = resetToken;
      this.page = 'password-reset';
    }
    this.loadPublications();
    this.loadCreditPacks();
  }

  private restoreSession(): void {
    const stored = localStorage.getItem(AppComponent.sessionStorageKey);
    if (!stored) {
      return;
    }
    try {
      const value = JSON.parse(stored) as { token?: string; user?: UserSession };
      if (!value.token || !value.user) {
        localStorage.removeItem(AppComponent.sessionStorageKey);
        return;
      }
      this.token = value.token;
      this.session = value.user;
      this.api.setToken(value.token);
      this.fillProfileForm(value.user);
      this.loadCredits();
      this.loadCreditMovements();
      this.loadStatistics();
      if (this.isAdmin) {
        this.loadInstitutions();
        this.loadAdminData();
      }
    } catch {
      localStorage.removeItem(AppComponent.sessionStorageKey);
    }
  }

  private storeSession(response: AuthResponse): void {
    this.token = response.token;
    this.api.setToken(response.token);
    this.session = response.user;
    localStorage.setItem(AppComponent.sessionStorageKey, JSON.stringify(response));
  }

  navigate(page: Page): void {
    this.page = page;
    if (page === 'administration') {
      this.loadInstitutions();
      this.loadAdminData();
    }
  }

  setLanguage(language: Language): void {
    this.language = language;
  }

  t(key: TranslationKey): string {
    return translations[this.language][key];
  }

  get navigationLabels(): NavigationLabels {
    return {
      title: this.t('title'),
      catalogue: this.t('catalogue'),
      newPublication: this.t('newPublication'),
      administration: this.t('administration'),
      profile: this.t('profile'),
      login: this.t('login'),
      register: this.t('register'),
      language: this.t('language')
    };
  }

  get publicationCardLabels(): PublicationCardLabels {
    return {
      consult: this.t('consult'),
      extract: this.t('extract'),
      edit: this.t('editMetadata'),
      delete: this.t('deletePublication'),
      processing: this.t('processing')
    };
  }

  get publicationDetailLabels(): PublicationDetailLabels {
    return {
      publicationDetails: this.t('publicationDetails'),
      backToCatalogue: this.t('backToCatalogue'),
      noSummary: this.t('noSummary'),
      publicationDate: this.t('publicationDate'),
      language: this.t('language'),
      documentType: this.t('documentType'),
      classification: this.t('classification'),
      status: this.t('status'),
      visibility: this.t('visibility'),
      downloadFile: this.t('downloadFile'),
      noFile: this.t('noFile')
    };
  }

  loadPublications(clearMessage = true): void {
    this.loading = true;
    const request = this.hasSearchFilters()
      ? this.api.searchPublications(this.search, this.searchFilters)
      : this.api.getPublications(this.search);
    request.subscribe({
      next: (publications) => {
        this.publications = publications;
        if (this.selectedPublication) {
          const updated = publications.find((publication) => publication.id === this.selectedPublication?.id);
          this.selectedPublication = updated ?? this.selectedPublication;
        }
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

  clearSearchFilters(): void {
    this.searchFilters = {
      author: '',
      language: '',
      documentType: '',
      startDate: '',
      endDate: ''
    };
    this.loadPublications();
  }

  openPublication(publication: Publication): void {
    this.selectedPublication = publication;
    this.page = 'detail';
    this.message = '';
    this.api.getPublication(publication.id).subscribe({
      next: (details) => {
        this.selectedPublication = details;
      },
      error: () => {
        this.message = this.t('apiUnavailable');
      }
    });
  }

  backToCatalogue(): void {
    this.page = 'catalogue';
    this.selectedPublication = null;
  }

  openPasswordReset(): void {
    this.page = 'password-reset';
    this.passwordResetRequested = false;
    this.message = '';
  }

  requestPasswordReset(): void {
    this.api.requestPasswordReset(this.passwordResetForm.email).subscribe({
      next: () => {
        this.passwordResetRequested = true;
      },
      error: () => {
        this.message = this.t('apiUnavailable');
      }
    });
  }

  confirmPasswordReset(): void {
    this.api.confirmPasswordReset(this.passwordResetForm.token, this.passwordResetForm.password).subscribe({
      next: () => {
        this.page = 'connexion';
        this.passwordResetForm = { email: '', token: '', password: '' };
        this.message = this.t('passwordResetDone');
      },
      error: () => {
        this.message = this.t('apiUnavailable');
      }
    });
  }

  login(): void {
    this.api.login(this.loginForm).subscribe({
      next: (response) => {
        this.storeSession(response);
        this.fillProfileForm(response.user);
        this.deletionRequested = false;
        this.profileSaved = false;
        this.message = '';
        this.page = 'profil';
        this.loadCredits();
        this.loadCreditMovements();
        this.loadStatistics();
        if (this.isAdmin) {
          this.loadInstitutions();
          this.loadAdminData();
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
        this.storeSession(response);
        this.fillProfileForm(response.user);
        this.deletionRequested = false;
        this.profileSaved = false;
        this.message = '';
        this.page = 'profil';
        this.loadCredits();
        this.loadCreditMovements();
        this.loadStatistics();
      },
      error: () => {
        this.message = this.t('registerFailed');
      }
    });
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
      keywords: this.publicationForm.keywords.split(',').map((keyword) => keyword.trim()).filter(Boolean),
      image: this.publicationForm.image
    }).subscribe({
      next: () => {
        this.publicationForm = {
          title: '',
          author: '',
          year: new Date().getFullYear(),
          visibility: 'PUBLIC',
          keywords: '',
          image: null
        };
        this.page = 'catalogue';
        this.loadPublications();
      },
      error: () => {
        this.message = this.t('createPublicationFailed');
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.importForm.file = input.files?.[0] ?? null;
  }

  onPublicationImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.publicationForm.image = input.files?.[0] ?? null;
  }

  onImportImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.importForm.image = input.files?.[0] ?? null;
  }

  importDocument(): void {
    if (!this.session || !this.importForm.file) {
      this.message = this.t('invalidForm');
      return;
    }

    this.api.importDocument(this.importForm.file, this.importForm.visibility, this.importForm.image).subscribe({
      next: (publication) => {
        this.importForm = { file: null, image: null, visibility: 'INSTITUTION' };
        this.page = 'catalogue';
        this.message = this.t('importQueued');
        this.loadPublications();
        this.refreshImportStatus(publication.id);
      },
      error: () => {
        this.message = this.t('createPublicationFailed');
      }
    });
  }

  private refreshImportStatus(publicationId: number, attempts = 0): void {
    if (attempts >= 5) {
      return;
    }
    window.setTimeout(() => {
      this.api.getPublication(publicationId).subscribe({
        next: (publication) => {
          const index = this.publications.findIndex((item) => item.id === publicationId);
          if (index >= 0) {
            this.publications[index] = publication;
          }
          if (publication.status === 'EN_ATTENTE' || publication.status === 'EXTRACTION') {
            this.refreshImportStatus(publicationId, attempts + 1);
          }
        },
        error: () => undefined
      });
    }, 2000);
  }

  loadCredits(): void {
    if (!this.session) {
      this.creditBalance = null;
      return;
    }

    this.api.getCreditAccount().subscribe({
      next: (account) => {
        this.creditBalance = account.balance.balance;
        this.creditMovements = account.movements;
      },
      error: () => {
        this.creditBalance = null;
      }
    });
  }

  loadCreditMovements(): void {
    if (!this.session) {
      this.creditMovements = [];
      return;
    }

    this.api.getCreditMovements(this.session.id).subscribe({
      next: (movements) => {
        this.creditMovements = movements;
      },
      error: () => {
        this.creditMovements = [];
      }
    });
  }

  loadCreditPacks(): void {
    this.api.getCreditPacks().subscribe({
      next: (packs) => {
        this.creditPacks = packs;
      },
      error: () => {
        this.creditPacks = [];
      }
    });
  }

  loadStatistics(): void {
    if (!this.session) {
      this.statistics = null;
      return;
    }

    this.api.getStatistics().subscribe({
      next: (statistics) => {
        this.statistics = statistics;
        this.creditBalance = statistics.creditBalance;
      },
      error: () => {
        this.statistics = null;
      }
    });
  }

  purchaseCredits(packId: number): void {
    if (!this.session) {
      return;
    }

    this.api.startCreditCheckout(packId).subscribe({
      next: (checkout) => {
        const pack = this.creditPacks.find((item) => item.id === packId);
        if (pack && pack.amount > 0) {
          window.location.assign(checkout.checkout_url);
          return;
        }
        this.api.confirmCreditPayment(checkout.reference).subscribe({
          next: (credits) => {
            this.creditBalance = credits.balance;
            this.loadCreditMovements();
            this.loadStatistics();
            this.message = this.t('checkoutStarted');
          },
          error: () => {
            this.message = this.t('purchaseFailed');
          }
        });
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
        this.loadCreditMovements();
        this.loadStatistics();
        this.loadPublications();
        this.startMetadataValidation(publication);
      },
      error: () => {
        this.message = this.t('extractionFailed');
      }
    });
  }

  startMetadataValidation(publication: Publication): void {
    if (!this.canManagePublication(publication)) {
      this.message = this.t('statusUpdateFailed');
      return;
    }

    this.api.getMetadata(publication.id).subscribe({
      next: (metadata) => {
        this.fillMetadataForm(metadata, publication);
        this.message = this.t('metadataLoaded');
      },
      error: () => {
        this.metadataForm = {
          documentId: publication.id,
          title: publication.title,
          summary: '',
          publicationDate: publication.year > 0 ? `${publication.year}-01-01` : '',
          classification: '',
          visibility: publication.visibility,
          authors: publication.author,
          keywords: publication.keywords.join(', '),
          extractedText: ''
        };
        this.selectedMetadataPublicationId = publication.id;
      }
    });
  }

  validateMetadata(): void {
    if (!this.session || !this.selectedMetadataPublicationId || !this.isMetadataFormValid()) {
      this.message = this.t('invalidForm');
      return;
    }

    this.api.validateMetadata(this.selectedMetadataPublicationId, {
      titre: this.metadataForm.title.trim(),
      resume: this.metadataForm.summary.trim(),
      date_publication: this.metadataForm.publicationDate || null,
      classification: this.metadataForm.classification.trim(),
      visibilite: this.metadataForm.visibility,
      auteurs: this.metadataForm.authors.split(',')
        .map((author) => author.trim())
        .filter(Boolean)
        .map((author) => ({ nom_complet: author })),
      mots_cles: this.metadataForm.keywords.split(',')
        .map((keyword) => keyword.trim())
        .filter(Boolean)
    }).subscribe({
      next: () => {
        this.cancelMetadataValidation();
        this.message = this.t('publicationPublished');
        this.loadStatistics();
        this.loadPublications(false);
      },
      error: () => {
        this.message = this.t('statusUpdateFailed');
      }
    });
  }

  cancelMetadataValidation(): void {
    this.selectedMetadataPublicationId = null;
    this.metadataForm = {
      documentId: 0,
      title: '',
      summary: '',
      publicationDate: '',
      classification: '',
      visibility: 'PUBLIC',
      authors: '',
      keywords: '',
      extractedText: ''
    };
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
        this.loadStatistics();
        this.loadPublications(false);
      },
      error: () => {
        this.message = this.t('statusUpdateFailed');
      }
    });
  }

  deletePublication(publication: Publication): void {
    if (!this.canManagePublication(publication)) {
      this.message = this.t('statusUpdateFailed');
      return;
    }

    this.api.deletePublication(publication.id).subscribe({
      next: (updatedPublication) => {
        this.publications = this.publications.map((item) => item.id === updatedPublication.id ? updatedPublication : item);
        this.message = this.t('publicationDeleted');
        this.loadStatistics();
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
    localStorage.removeItem(AppComponent.sessionStorageKey);
    this.institutions = [];
    this.adminUsers = [];
    this.adminConfig = {};
    this.adminConfigForm = {};
    this.auditLogs = [];
    this.creditBalance = null;
    this.creditMovements = [];
    this.statistics = null;
    this.extractionResult = null;
    this.selectedPublication = null;
    this.deletionRequested = false;
    this.profileSaved = false;
    this.page = 'catalogue';
  }

  get isAdmin(): boolean {
    return this.session?.role === 'ADMIN';
  }

  loadAdminData(): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.getAdminUsers().subscribe({
      next: (users) => {
        this.adminUsers = users;
      },
      error: () => {
        this.adminUsers = [];
      }
    });
    this.api.getAdminConfig().subscribe({
      next: (config) => {
        this.adminConfig = config;
        this.adminConfigForm = { ...config };
      },
      error: () => {
        this.adminConfig = {};
        this.adminConfigForm = {};
      }
    });
    this.api.getAdminLogs().subscribe({
      next: (logs) => {
        this.auditLogs = logs;
      },
      error: () => {
        this.auditLogs = [];
      }
    });
  }

  deactivateUser(user: UserSession): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.updateAdminUser(user.id, { role: user.role === 'ADMIN' ? 'ADMIN' : 'LIBRARIAN', statut: 'DESACTIVE' }).subscribe({
      next: () => this.loadAdminData(),
      error: () => {
        this.message = this.t('statusUpdateFailed');
      }
    });
  }

  saveAdminConfig(): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.updateAdminConfig(this.adminConfigForm).subscribe({
      next: (config) => {
        this.adminConfig = config;
        this.adminConfigForm = { ...config };
        this.message = this.t('configurationSaved');
        this.loadAdminData();
      },
      error: () => {
        this.message = this.t('configurationFailed');
      }
    });
  }

  exportAdminDocumentsCsv(): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.exportAdminDocumentsCsv().subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'metamind-documents.csv';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.message = this.t('exportFailed');
      }
    });
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

  isMetadataFormValid(): boolean {
    return this.metadataForm.title.trim().length >= 3
      && this.metadataForm.authors.trim().length >= 2
      && this.metadataForm.keywords.trim().length >= 2;
  }

  isRegisterFormValid(): boolean {
    return this.registerForm.firstName.trim().length >= 2
      && this.registerForm.lastName.trim().length >= 2
      && this.registerForm.email.includes('@')
      && this.registerForm.institution.trim().length >= 2
      && this.registerForm.password.length >= 8;
  }

  hasSearchFilters(): boolean {
    return !!this.searchFilters.author?.trim()
      || !!this.searchFilters.language?.trim()
      || !!this.searchFilters.documentType?.trim()
      || !!this.searchFilters.startDate
      || !!this.searchFilters.endDate;
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

  private fillMetadataForm(metadata: MetadataDetails, publication: Publication): void {
    this.metadataForm = {
      documentId: metadata.document_id,
      title: metadata.titre || publication.title,
      summary: metadata.resume || '',
      publicationDate: metadata.date_publication || (publication.year > 0 ? `${publication.year}-01-01` : ''),
      classification: metadata.classification || '',
      visibility: metadata.visibilite || publication.visibility,
      authors: metadata.auteurs.length > 0 ? metadata.auteurs.map((author) => author.nom_complet).join(', ') : publication.author,
      keywords: metadata.mots_cles.length > 0 ? metadata.mots_cles.join(', ') : publication.keywords.join(', '),
      extractedText: metadata.texte_extrait || ''
    };
    this.selectedMetadataPublicationId = publication.id;
  }
}

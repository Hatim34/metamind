import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Meta, Title } from '@angular/platform-browser';

import { ApiService, AuditLog, AuthResponse, CreditMovement, CreditPackOption, DashboardStatistics, ExtractionQuality, Institution, MetadataDetails, MetadataExtraction, Publication, PublicationStatus, ReferenceData, SearchFilters, UserSession } from './api.service';
import { NavigationLabels, NavbarComponent } from './navbar.component';
import { PublicationCardLabels, PublicationCardComponent } from './publication-card.component';
import { PublicationDetailComponent, PublicationDetailLabels } from './publication-detail.component';

type Page = 'accueil' | 'catalogue' | 'detail' | 'connexion' | 'inscription' | 'profil' | 'publication' | 'validation' | 'administration' | 'password-reset' | 'legal';
type Language = 'fr' | 'nl' | 'en';

const translations = {
  fr: {
    title: 'Gestion des métadonnées académiques',
    homeTitle: 'Les publications des universités, décrites et vérifiées.',
    homeDescription: 'Articles, thèses et rapports en libre accès. Chaque notice est proposée par une IA puis relue par un bibliothécaire.',
    homeSearchPlaceholder: 'Titre, auteur, mot-clé, DOI…',
    exploreCatalogue: 'Explorer le catalogue',
    openPublications: 'publications en libre accès',
    representedInstitutions: 'institutions représentées',
    latestPublications: 'Dernières publications',
    catalogue: 'Catalogue',
    login: 'Connexion',
    register: 'Inscription',
    profile: 'Profil',
    newPublication: 'Nouvelle publication',
    validationQueue: 'File de validation',
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
    noPublications: 'Aucune publication ne correspond à votre recherche.',
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
    retryProcessing: 'Relancer le traitement',
    downloadFailed: 'Le fichier ne peut pas être consulté avec ce compte.',
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
    loginIntro: 'Espace réservé aux bibliothécaires et administrateurs.',
    professionalEmail: 'Adresse e-mail professionnelle',
    professionalEmailPlaceholder: 'nom@institution.be',
    requestAccessPrompt: 'Vous n’avez pas encore de compte ?',
    requestAccessLink: 'Demander un accès',
    activationDelay: 'Les demandes d’accès sont validées par l’administrateur de votre institution.',
    workflowTitle: 'Des métadonnées fiables, sous votre contrôle.',
    workflowImport: 'Importer',
    workflowImportText: 'Déposez une publication et son contenu est préparé pour l’analyse.',
    workflowReview: 'Relire',
    workflowReviewText: 'Vérifiez chaque suggestion générée avant de la conserver.',
    workflowPublish: 'Publier',
    workflowPublishText: 'Diffusez une fiche complète dans le catalogue de votre institution.',
    workflowNote: 'L’IA assiste la saisie. La décision finale reste toujours humaine.',
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
    paymentTerms: 'J accepte les conditions generales de vente.',
    paymentWithdrawalWaiver: 'Je demande l execution immediate du service numerique et renonce au droit de retractation.',
    checkoutStarted: 'Paiement confirme, le solde est mis a jour.',
    importFile: 'Importer un document',
    selectedFile: 'Fichier sélectionné',
    coverImage: 'Image de couverture',
    sendFile: 'Importer le fichier',
    importQueued: 'Import en cours de traitement.',
    importing: 'Import du fichier en cours...',
    importCompleted: 'Le fichier a été importé. Le traitement peut commencer.',
    importFailed: 'Import du fichier impossible.',
    adminUsers: 'Utilisateurs',
    configuration: 'Configuration',
    saveConfiguration: 'Enregistrer la configuration',
    configurationSaved: 'La configuration est mise à jour.',
    configurationFailed: 'Modification de la configuration impossible.',
    exportCsv: 'Exporter le CSV',
    exportFailed: 'Export CSV impossible.',
    auditLogs: 'Journal d audit',
    deactivateAccount: 'Désactiver',
    activateAccount: 'Activer',
    accountActivated: 'Le compte est active.',
    roleUpdated: 'Le rôle a été mis à jour.',
    legalCenter: 'Informations legales',
    privacyTitle: 'Politique de confidentialite',
    legalNoticeTitle: 'Mentions legales',
    termsTitle: 'Conditions generales de vente',
    requestDeletion: 'Demander la suppression du compte',
    exportMyData: 'Exporter mes données',
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
    metadataTitle: 'Titre',
    metadataSummary: 'Résumé',
    metadataClassification: 'Classification',
    metadataAuthors: 'Auteurs',
    metadataKeywords: 'Mots-clés',
    metadataLanguage: 'Langue',
    metadataDocumentType: 'Type de document',
    metadataDoi: 'DOI',
    metadataPublicationDate: 'Date de publication',
    averageProcessingLabel: 'Temps moyen import vers publication',
    languageNotSet: 'Non renseignée',
    documentTypeNotSet: 'Non renseigné',
    doiPlaceholderHint: 'Identifiant DOI, sans https://doi.org/',
    qualityHeading: 'Fiabilité mesurée de l\'IA',
    qualityIntro: 'Calculé sur les décisions réelles des bibliothécaires, champ par champ.',
    qualityNoData: 'Aucune métadonnée validée pour le moment : les indicateurs apparaîtront après la première validation.',
    qualityArbitrated: 'Suggestions arbitrées',
    qualityOverallAcceptance: 'Taux d\'acceptation global',
    qualityField: 'Champ',
    qualityAccepted: 'Acceptées',
    qualityModified: 'Corrigées',
    qualityEmptied: 'Effacées',
    qualityAcceptanceRate: 'Taux d\'acceptation',
    qualityEditDistance: 'Ampleur des corrections',
    qualityCalibration: 'Calibration du score de confiance',
    qualityCalibrationIntro: 'Si le score est fiable, le taux d\'acceptation doit augmenter d\'une tranche à la suivante.',
    qualityRange: 'Tranche de score',
    summary: 'Résumé',
    extractedText: 'Texte extrait',
    noExtractedText: 'Aucun texte extrait disponible.',
    publicationDate: 'Date de publication',
    classification: 'Classification',
    cancel: 'Annuler',
    confirm: 'Confirmer',
    confirmDeleteQuestion: 'Supprimer cette publication ?',
    stepImported: 'Importé',
    stepExtraction: 'Extraction IA',
    stepToValidate: 'À valider',
    stepPublished: 'Publié',
    rejectMetadata: 'Rejeter',
    rejectReasonLabel: 'Motif du rejet',
    rejectReasonRequired: 'Le motif du rejet est obligatoire.',
    metadataRejected: 'Les métadonnées ont été rejetées.',
    deletePublication: 'Supprimer',
    apiUnavailable: "Impossible de joindre l'API locale.",
    loginFailed: 'Connexion impossible avec les donnees envoyees.',
    registerFailed: 'Creation du compte impossible avec les donnees envoyees.',
    registrationPending: 'Votre compte a ete cree. Il doit etre valide par un administrateur avant votre premiere connexion.',
    createPublicationFailed: 'Creation de la publication impossible avec les donnees envoyees.',
    purchaseFailed: 'Achat de credits impossible.',
    paymentConsentRequired: 'Acceptez les CGV et la demande d execution immediate avant de continuer.',
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
    invalidForm: 'Veuillez compléter correctement les champs obligatoires.',
    sending: 'Envoi en cours...',
    publicationAdded: 'Publication ajoutée au catalogue.',
    fileTooLarge: 'Fichier trop volumineux (50 Mo maximum).',
    imageTooLarge: 'Image trop volumineuse (5 Mo maximum).',
    unsupportedFileType: 'Format non pris en charge. Formats acceptés : PDF, DOCX, TXT.',
    unsupportedImageType: 'Image non prise en charge. Formats acceptés : PNG, JPG, WEBP.',
    sessionExpired: 'Session expirée. Reconnectez-vous.',
    documentsToProcess: 'Documents à traiter',
    noValidationDocuments: 'Aucun document à traiter pour le moment.',
    filterByTitleOrAuthor: 'Filtrer par titre ou auteur',
    documentColumn: 'Document',
    institutionColumn: 'Institution',
    confidenceByField: 'Confiance par champ',
    confidenceNotCalculated: 'Confiance non calculée',
    moreActions: 'Plus d’actions',
    reviewMetadataIntro: 'Vérifiez les champs proposés avant de publier cette notice.',
    backToQueue: 'Retour à la file',
    refresh: 'Actualiser',
    allStatuses: 'Tous les statuts',
    statusPending: 'En attente',
    noAuditLog: "Aucune entrée dans le journal d'audit."
  },
  nl: {
    title: 'Beheer van academische metadata',
    homeTitle: 'Universitaire publicaties, beschreven en gecontroleerd.',
    homeDescription: 'Artikelen, proefschriften en rapporten in open access. Elke fiche wordt voorgesteld door AI en nagelezen door een bibliothecaris.',
    homeSearchPlaceholder: 'Titel, auteur, trefwoord, DOI…',
    exploreCatalogue: 'Catalogus verkennen',
    openPublications: 'open-accesspublicaties',
    representedInstitutions: 'vertegenwoordigde instellingen',
    latestPublications: 'Laatste publicaties',
    catalogue: 'Catalogus',
    login: 'Aanmelden',
    register: 'Registreren',
    profile: 'Profiel',
    newPublication: 'Nieuwe publicatie',
    validationQueue: 'Validatiewachtrij',
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
    noPublications: 'Geen publicatie komt overeen met uw zoekopdracht.',
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
    retryProcessing: 'Verwerking opnieuw starten',
    downloadFailed: 'Het bestand kan niet met deze account worden bekeken.',
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
    loginIntro: 'Ruimte voor bibliothecarissen en beheerders.',
    professionalEmail: 'Professioneel e-mailadres',
    professionalEmailPlaceholder: 'naam@instelling.be',
    requestAccessPrompt: 'Nog geen account?',
    requestAccessLink: 'Toegang aanvragen',
    activationDelay: 'Aanvragen worden gevalideerd door de beheerder van uw instelling.',
    workflowTitle: 'Betrouwbare metadata, onder uw controle.',
    workflowImport: 'Importeren',
    workflowImportText: 'Voeg een publicatie toe; de inhoud wordt voorbereid voor analyse.',
    workflowReview: 'Nakijken',
    workflowReviewText: 'Controleer elke gegenereerde suggestie voordat u die bewaart.',
    workflowPublish: 'Publiceren',
    workflowPublishText: 'Publiceer een volledige fiche in de catalogus van uw instelling.',
    workflowNote: 'AI ondersteunt de invoer. De uiteindelijke beslissing blijft menselijk.',
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
    paymentTerms: 'Ik aanvaard de algemene verkoopvoorwaarden.',
    paymentWithdrawalWaiver: 'Ik vraag de onmiddellijke uitvoering van de digitale dienst en doe afstand van het herroepingsrecht.',
    checkoutStarted: 'Betaling bevestigd, het saldo is bijgewerkt.',
    importFile: 'Document importeren',
    selectedFile: 'Geselecteerd bestand',
    coverImage: 'Omslagafbeelding',
    sendFile: 'Bestand importeren',
    importQueued: 'De import wordt verwerkt.',
    importing: 'Het bestand wordt geïmporteerd...',
    importCompleted: 'Het bestand is geïmporteerd. De verwerking kan starten.',
    importFailed: 'Het bestand kan niet worden geïmporteerd.',
    adminUsers: 'Gebruikers',
    configuration: 'Configuratie',
    saveConfiguration: 'Configuratie opslaan',
    configurationSaved: 'De configuratie is bijgewerkt.',
    configurationFailed: 'Configuratie wijzigen is onmogelijk.',
    exportCsv: 'CSV exporteren',
    exportFailed: 'CSV-export is onmogelijk.',
    auditLogs: 'Auditlogboek',
    deactivateAccount: 'Deactiveren',
    activateAccount: 'Activeren',
    accountActivated: 'Het account is geactiveerd.',
    roleUpdated: 'De rol is bijgewerkt.',
    legalCenter: 'Juridische informatie',
    privacyTitle: 'Privacybeleid',
    legalNoticeTitle: 'Wettelijke vermeldingen',
    termsTitle: 'Algemene verkoopvoorwaarden',
    requestDeletion: 'Verwijdering van de account aanvragen',
    exportMyData: 'Mijn gegevens exporteren',
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
    metadataTitle: 'Titel',
    metadataSummary: 'Samenvatting',
    metadataClassification: 'Classificatie',
    metadataAuthors: 'Auteurs',
    metadataKeywords: 'Trefwoorden',
    metadataLanguage: 'Taal',
    metadataDocumentType: 'Documenttype',
    metadataDoi: 'DOI',
    metadataPublicationDate: 'Publicatiedatum',
    averageProcessingLabel: 'Gemiddelde tijd van import tot publicatie',
    languageNotSet: 'Niet ingevuld',
    documentTypeNotSet: 'Niet ingevuld',
    doiPlaceholderHint: 'DOI-identificatie, zonder https://doi.org/',
    qualityHeading: 'Gemeten betrouwbaarheid van de AI',
    qualityIntro: 'Berekend op de werkelijke beslissingen van de bibliothecarissen, veld per veld.',
    qualityNoData: 'Nog geen gevalideerde metadata: de indicatoren verschijnen na de eerste validatie.',
    qualityArbitrated: 'Beoordeelde suggesties',
    qualityOverallAcceptance: 'Globaal aanvaardingspercentage',
    qualityField: 'Veld',
    qualityAccepted: 'Aanvaard',
    qualityModified: 'Gecorrigeerd',
    qualityEmptied: 'Gewist',
    qualityAcceptanceRate: 'Aanvaardingspercentage',
    qualityEditDistance: 'Omvang van de correcties',
    qualityCalibration: 'Kalibratie van de betrouwbaarheidsscore',
    qualityCalibrationIntro: 'Als de score betrouwbaar is, moet het aanvaardingspercentage per schijf stijgen.',
    qualityRange: 'Scoreschijf',
    summary: 'Samenvatting',
    extractedText: 'Geextraheerde tekst',
    noExtractedText: 'Geen geextraheerde tekst beschikbaar.',
    publicationDate: 'Publicatiedatum',
    classification: 'Classificatie',
    cancel: 'Annuleren',
    confirm: 'Bevestigen',
    confirmDeleteQuestion: 'Deze publicatie verwijderen?',
    stepImported: 'Geïmporteerd',
    stepExtraction: 'AI-extractie',
    stepToValidate: 'Te valideren',
    stepPublished: 'Gepubliceerd',
    rejectMetadata: 'Weigeren',
    rejectReasonLabel: 'Reden voor weigering',
    rejectReasonRequired: 'De reden voor weigering is verplicht.',
    metadataRejected: 'De metadata zijn geweigerd.',
    deletePublication: 'Verwijderen',
    apiUnavailable: 'De lokale API is niet bereikbaar.',
    loginFailed: 'Aanmelden is onmogelijk met de verzonden gegevens.',
    registerFailed: 'Account aanmaken is onmogelijk met de verzonden gegevens.',
    registrationPending: 'Uw account is aangemaakt. Het moet door een beheerder worden gevalideerd voor uw eerste aanmelding.',
    createPublicationFailed: 'Publicatie aanmaken is onmogelijk met de verzonden gegevens.',
    purchaseFailed: 'Credits kopen is onmogelijk.',
    paymentConsentRequired: 'Accepteer de voorwaarden en de onmiddellijke uitvoering om verder te gaan.',
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
    invalidForm: 'Vul de verplichte velden correct in.',
    sending: 'Bezig met verzenden...',
    publicationAdded: 'Publicatie toegevoegd aan de catalogus.',
    fileTooLarge: 'Bestand te groot (max. 50 MB).',
    imageTooLarge: 'Afbeelding te groot (max. 5 MB).',
    unsupportedFileType: 'Formaat niet ondersteund. Toegestaan: PDF, DOCX, TXT.',
    unsupportedImageType: 'Afbeelding niet ondersteund. Toegestaan: PNG, JPG, WEBP.',
    sessionExpired: 'Sessie verlopen. Meld u opnieuw aan.',
    documentsToProcess: 'Te verwerken documenten',
    noValidationDocuments: 'Voorlopig geen documenten om te verwerken.',
    filterByTitleOrAuthor: 'Filter op titel of auteur',
    documentColumn: 'Document',
    institutionColumn: 'Instelling',
    confidenceByField: 'Betrouwbaarheid per veld',
    confidenceNotCalculated: 'Betrouwbaarheid niet berekend',
    moreActions: 'Meer acties',
    reviewMetadataIntro: 'Controleer de voorgestelde velden voordat u deze fiche publiceert.',
    backToQueue: 'Terug naar de wachtrij',
    refresh: 'Vernieuwen',
    allStatuses: 'Alle statussen',
    statusPending: 'In afwachting',
    noAuditLog: 'Geen vermelding in het auditlogboek.'
  },
  en: {
    title: 'Academic metadata management',
    homeTitle: 'University publications, described and verified.',
    homeDescription: 'Open-access articles, theses and reports. Each record is proposed by AI and reviewed by a librarian.',
    homeSearchPlaceholder: 'Title, author, keyword, DOI…',
    exploreCatalogue: 'Explore the catalogue',
    openPublications: 'open-access publications',
    representedInstitutions: 'represented institutions',
    latestPublications: 'Latest publications',
    catalogue: 'Catalogue',
    login: 'Sign in',
    register: 'Register',
    profile: 'Profile',
    newPublication: 'New publication',
    validationQueue: 'Validation queue',
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
    noPublications: 'No publication matches your search.',
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
    retryProcessing: 'Retry processing',
    downloadFailed: 'The file cannot be viewed with this account.',
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
    loginIntro: 'Space reserved for librarians and administrators.',
    professionalEmail: 'Professional email address',
    professionalEmailPlaceholder: 'name@institution.org',
    requestAccessPrompt: 'Do not have an account yet?',
    requestAccessLink: 'Request access',
    activationDelay: 'Access requests are approved by your institution administrator.',
    workflowTitle: 'Reliable metadata, under your control.',
    workflowImport: 'Import',
    workflowImportText: 'Add a publication and prepare its content for analysis.',
    workflowReview: 'Review',
    workflowReviewText: 'Check every generated suggestion before keeping it.',
    workflowPublish: 'Publish',
    workflowPublishText: 'Share a complete record in your institution catalogue.',
    workflowNote: 'AI assists data entry. The final decision always remains human.',
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
    paymentTerms: 'I accept the terms and conditions of sale.',
    paymentWithdrawalWaiver: 'I request immediate execution of the digital service and waive the right of withdrawal.',
    checkoutStarted: 'Payment confirmed, the balance is updated.',
    importFile: 'Import a document',
    selectedFile: 'Selected file',
    coverImage: 'Cover image',
    sendFile: 'Import file',
    importQueued: 'The import is being processed.',
    importing: 'The file is being imported...',
    importCompleted: 'The file was imported. Processing can start.',
    importFailed: 'The file could not be imported.',
    adminUsers: 'Users',
    configuration: 'Configuration',
    saveConfiguration: 'Save configuration',
    configurationSaved: 'The configuration is updated.',
    configurationFailed: 'Configuration update failed.',
    exportCsv: 'Export CSV',
    exportFailed: 'CSV export failed.',
    auditLogs: 'Audit log',
    deactivateAccount: 'Deactivate',
    activateAccount: 'Activate',
    accountActivated: 'The account has been activated.',
    roleUpdated: 'The role has been updated.',
    legalCenter: 'Legal information',
    privacyTitle: 'Privacy policy',
    legalNoticeTitle: 'Legal notice',
    termsTitle: 'Terms and conditions of sale',
    requestDeletion: 'Request account deletion',
    exportMyData: 'Export my data',
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
    metadataTitle: 'Title',
    metadataSummary: 'Summary',
    metadataClassification: 'Classification',
    metadataAuthors: 'Authors',
    metadataKeywords: 'Keywords',
    metadataLanguage: 'Language',
    metadataDocumentType: 'Document type',
    metadataDoi: 'DOI',
    metadataPublicationDate: 'Publication date',
    averageProcessingLabel: 'Average time from import to publication',
    languageNotSet: 'Not set',
    documentTypeNotSet: 'Not set',
    doiPlaceholderHint: 'DOI identifier, without https://doi.org/',
    qualityHeading: 'Measured reliability of the AI',
    qualityIntro: 'Computed from the librarians\' actual decisions, field by field.',
    qualityNoData: 'No validated metadata yet: the indicators appear after the first validation.',
    qualityArbitrated: 'Reviewed suggestions',
    qualityOverallAcceptance: 'Overall acceptance rate',
    qualityField: 'Field',
    qualityAccepted: 'Accepted',
    qualityModified: 'Corrected',
    qualityEmptied: 'Cleared',
    qualityAcceptanceRate: 'Acceptance rate',
    qualityEditDistance: 'Extent of corrections',
    qualityCalibration: 'Confidence score calibration',
    qualityCalibrationIntro: 'If the score is reliable, the acceptance rate should rise from one band to the next.',
    qualityRange: 'Score band',
    summary: 'Summary',
    extractedText: 'Extracted text',
    noExtractedText: 'No extracted text available.',
    publicationDate: 'Publication date',
    classification: 'Classification',
    cancel: 'Cancel',
    confirm: 'Confirm',
    confirmDeleteQuestion: 'Delete this publication?',
    stepImported: 'Imported',
    stepExtraction: 'AI extraction',
    stepToValidate: 'To validate',
    stepPublished: 'Published',
    rejectMetadata: 'Reject',
    rejectReasonLabel: 'Rejection reason',
    rejectReasonRequired: 'The rejection reason is required.',
    metadataRejected: 'The metadata were rejected.',
    deletePublication: 'Delete',
    apiUnavailable: 'Unable to reach the local API.',
    loginFailed: 'Sign-in failed with the submitted data.',
    registerFailed: 'Account creation failed with the submitted data.',
    registrationPending: 'Your account has been created. It must be validated by an administrator before your first sign-in.',
    createPublicationFailed: 'Publication creation failed with the submitted data.',
    purchaseFailed: 'Credit purchase failed.',
    paymentConsentRequired: 'Accept the terms and immediate execution request before continuing.',
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
    invalidForm: 'Please complete the required fields correctly.',
    sending: 'Sending...',
    publicationAdded: 'Publication added to the catalogue.',
    fileTooLarge: 'File too large (50 MB maximum).',
    imageTooLarge: 'Image too large (5 MB maximum).',
    unsupportedFileType: 'Unsupported format. Accepted: PDF, DOCX, TXT.',
    unsupportedImageType: 'Unsupported image. Accepted: PNG, JPG, WEBP.',
    sessionExpired: 'Session expired. Please sign in again.',
    documentsToProcess: 'Documents to process',
    noValidationDocuments: 'No documents to process right now.',
    filterByTitleOrAuthor: 'Filter by title or author',
    documentColumn: 'Document',
    institutionColumn: 'Institution',
    confidenceByField: 'Confidence by field',
    confidenceNotCalculated: 'Confidence not calculated',
    moreActions: 'More actions',
    reviewMetadataIntro: 'Review the proposed fields before publishing this record.',
    backToQueue: 'Back to the queue',
    refresh: 'Refresh',
    allStatuses: 'All statuses',
    statusPending: 'Pending',
    noAuditLog: 'No audit log entry.'
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
  page: Page = 'accueil';
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
    language: '',
    documentType: '',
    doi: '',
    extractedText: '',
    rejectReason: ''
  };
  showRejectForm = false;

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
  validationQueue: Publication[] = [];
  validationFilter: 'all' | 'A_VALIDER' | 'EN_ATTENTE' | 'EXTRACTION' = 'all';
  validationSearch = '';
  selectedPublication: Publication | null = null;
  institutions: Institution[] = [];
  adminUsers: UserSession[] = [];
  adminConfig: Record<string, string> = {};
  adminConfigForm: Record<string, string> = {};
  auditLogs: AuditLog[] = [];
  creditPacks: CreditPackOption[] = [];
  creditBalance: number | null = null;
  creditMovements: CreditMovement[] = [];
  paymentTermsAccepted = false;
  paymentWithdrawalWaiverAccepted = false;
  importing = false;
  publicationSubmitting = false;
  publicationFeedback: { type: 'success' | 'error'; text: string } | null = null;
  importFeedback: { type: 'success' | 'error'; text: string } | null = null;
  statistics: DashboardStatistics | null = null;
  extractionQuality: ExtractionQuality | null = null;
  references: ReferenceData = { langues: [], types_documents: [] };
  extractionResult: MetadataExtraction | null = null;
  selectedMetadataPublicationId: number | null = null;
  extractingPublicationIds = new Set<number>();

  private readonly maxFileSize = 50 * 1024 * 1024;
  private readonly maxImageSize = 5 * 1024 * 1024;
  private readonly allowedFileExtensions = ['pdf', 'docx', 'txt'];
  private readonly allowedImageTypes = ['image/png', 'image/jpeg', 'image/webp'];

  constructor(
    private readonly api: ApiService,
    private readonly titleService: Title,
    private readonly metaService: Meta
  ) {}

  /** Met a jour le titre de l'onglet et la meta description (SEO on-page, livrable 17). */
  private updateSeo(title: string, description: string): void {
    const fullTitle = title ? `${title} · Metamind` : 'Metamind · Dépôt institutionnel de publications';
    this.titleService.setTitle(fullTitle);
    const clean = (description || 'Metamind, plateforme d\'extraction de métadonnées par intelligence artificielle pour les dépôts institutionnels.').slice(0, 300);
    this.metaService.updateTag({ name: 'description', content: clean });
  }

  ngOnInit(): void {
    this.restoreSession();
    const resetToken = new URLSearchParams(window.location.search).get('token');
    if (resetToken) {
      this.passwordResetForm.token = resetToken;
      this.page = 'password-reset';
    }
    this.loadPublications();
    this.loadCreditPacks();
    this.loadReferences();
    this.updateSeo(this.t(this.pageTitleKey(this.page)), '');
    try {
      window.history.replaceState({ page: this.page }, '', '#/' + this.page);
    } catch {
      // history indisponible : navigation en memoire uniquement
    }
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
      this.loadValidationQueue();
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
    this.applyPage(page);
    this.pushHistory({ page });
  }

  private applyPage(page: Page): void {
    this.page = page;
    if (page === 'validation') {
      this.loadValidationQueue();
      this.loadStatistics();
    }
    if (page === 'administration') {
      this.loadInstitutions();
      this.loadAdminData();
    }
    if (page !== 'detail') {
      this.updateSeo(this.t(this.pageTitleKey(page)), '');
    }
  }

  private pageTitleKey(page: Page): TranslationKey {
    switch (page) {
      case 'accueil': return 'title';
      case 'catalogue': return 'catalogue';
      case 'publication': return 'newPublication';
      case 'validation': return 'validationQueue';
      case 'administration': return 'administration';
      case 'profil': return 'profile';
      case 'connexion': return 'login';
      case 'inscription': return 'register';
      case 'legal': return 'legalCenter';
      default: return 'title';
    }
  }

  private pushHistory(state: { page: Page; publicationId?: number }): void {
    try {
      window.history.pushState(state, '', '#/' + state.page);
    } catch {
      // history indisponible : navigation en memoire uniquement
    }
  }

  @HostListener('window:popstate', ['$event'])
  onPopState(event: PopStateEvent): void {
    const state = (event.state ?? null) as { page?: Page; publicationId?: number } | null;
    const page = state?.page ?? 'accueil';
    if (page === 'detail' && state?.publicationId) {
      if (this.selectedPublication?.id === state.publicationId) {
        this.page = 'detail';
        this.updateSeo(this.selectedPublication.title, this.selectedPublication.summary || '');
        return;
      }
      this.api.getPublication(state.publicationId).subscribe({
        next: (details) => {
          this.selectedPublication = details;
          this.page = 'detail';
          this.updateSeo(details.title, details.summary || '');
        },
        error: () => this.applyPage('catalogue')
      });
      return;
    }
    if (page === 'detail') {
      this.applyPage('catalogue');
      return;
    }
    this.applyPage(page);
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
      validationQueue: this.t('validationQueue'),
      administration: this.t('administration'),
      profile: this.t('profile'),
      login: this.t('login'),
      register: this.t('register'),
      language: this.t('language'),
      signOut: this.t('logout')
    };
  }

  get publicationCardLabels(): PublicationCardLabels {
    return {
      consult: this.t('consult'),
      extract: this.t('extract'),
      edit: this.t('editMetadata'),
      delete: this.t('deletePublication'),
      processing: this.t('processing'),
      retry: this.t('retryProcessing'),
      stepImported: this.t('stepImported'),
      stepExtraction: this.t('stepExtraction'),
      stepToValidate: this.t('stepToValidate'),
      stepPublished: this.t('stepPublished'),
      confirmDelete: this.t('confirmDeleteQuestion'),
      confirm: this.t('confirm'),
      cancel: this.t('cancel'),
      documentType: this.t('documentType')
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
      noFile: this.t('noFile'),
      publicVisibility: this.t('publicVisibility'),
      institutionOnly: this.t('institutionOnly')
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

  loadValidationQueue(): void {
    if (!this.session) {
      this.validationQueue = [];
      return;
    }
    this.api.getManagedDocuments().subscribe({
      next: (documents) => {
        this.validationQueue = documents.filter((document) => document.status !== 'PUBLIE' && document.status !== 'SUPPRIME');
      },
      error: () => {
        this.validationQueue = [];
      }
    });
  }

  get filteredValidationQueue(): Publication[] {
    const query = this.validationSearch.trim().toLocaleLowerCase();
    return this.validationQueue.filter((document) => {
      const matchesStatus = this.validationFilter === 'all' || document.status === this.validationFilter;
      const matchesQuery = !query
        || document.title.toLocaleLowerCase().includes(query)
        || document.author.toLocaleLowerCase().includes(query);
      return matchesStatus && matchesQuery;
    });
  }

  get catalogueInstitutionCount(): number {
    return new Set(this.publications.map((publication) => publication.institution).filter(Boolean)).size;
  }

  openCatalogueSearch(): void {
    this.navigate('catalogue');
    this.loadPublications();
  }

  validationCount(status: Exclude<typeof this.validationFilter, 'all'>): number {
    return this.validationQueue.filter((document) => document.status === status).length;
  }

  documentTypeShort(publication: Publication): string {
    const type = (publication.documentType || '').toLocaleLowerCase();
    if (type.includes('th')) {
      return 'Th.';
    }
    if (type.includes('rapport')) {
      return 'Rap.';
    }
    if (type.includes('preprint')) {
      return 'Pre.';
    }
    return 'Art.';
  }

  documentTypeClass(publication: Publication): string {
    const type = (publication.documentType || '').toLocaleLowerCase();
    if (type.includes('th')) {
      return 'type-these';
    }
    if (type.includes('rapport')) {
      return 'type-rapport';
    }
    if (type.includes('preprint')) {
      return 'type-preprint';
    }
    return 'type-article';
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
    this.updateSeo(publication.title, publication.summary || `${publication.title}, ${publication.author}`);
    this.pushHistory({ page: 'detail', publicationId: publication.id });
    this.api.getPublication(publication.id).subscribe({
      next: (details) => {
        this.selectedPublication = details;
        this.updateSeo(details.title, details.summary || `${details.title}, ${details.author}`);
      },
      error: () => {
        this.message = this.t('apiUnavailable');
      }
    });
  }

  downloadPublicationFile(publication: Publication): void {
    if (!publication.fileUrl) {
      this.message = this.t('noFile');
      return;
    }
    this.api.downloadPublicationFile(publication.id).subscribe({
      next: (file) => {
        const url = URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${publication.title || 'document'}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.message = this.t('downloadFailed');
      }
    });
  }

  backToCatalogue(): void {
    this.selectedPublication = null;
    this.applyPage('catalogue');
    this.pushHistory({ page: 'catalogue' });
  }

  openLegal(): void {
    this.page = 'legal';
    this.message = '';
    this.pushHistory({ page: 'legal' });
    window.scrollTo({ top: 0 });
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
      next: () => {
        this.registerForm = { firstName: '', lastName: '', email: '', institution: '', password: '' };
        this.message = this.t('registrationPending');
        this.navigate('connexion');
      },
      error: () => {
        this.message = this.t('registerFailed');
      }
    });
  }

  createPublication(): void {
    if (!this.session) {
      this.publicationFeedback = { type: 'error', text: this.t('loginRequiredPublication') };
      return;
    }
    if (!this.isPublicationFormValid()) {
      this.publicationFeedback = { type: 'error', text: this.t('invalidForm') };
      this.message = this.t('invalidForm');
      return;
    }

    this.publicationSubmitting = true;
    this.publicationFeedback = null;
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
        this.publicationSubmitting = false;
        this.publicationForm = {
          title: '',
          author: '',
          year: new Date().getFullYear(),
          visibility: 'PUBLIC',
          keywords: '',
          image: null
        };
        this.publicationFeedback = null;
        this.message = this.t('publicationAdded');
        this.page = 'validation';
        this.loadPublications(false);
        this.loadValidationQueue();
      },
      error: (err) => {
        this.publicationSubmitting = false;
        this.publicationFeedback = { type: 'error', text: this.describeError(err, 'createPublicationFailed') };
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.importFeedback = null;
    if (file) {
      const validationError = this.validateDocumentFile(file);
      if (validationError) {
        this.importForm.file = null;
        input.value = '';
        this.importFeedback = { type: 'error', text: validationError };
        return;
      }
    }
    this.importForm.file = file;
  }

  onPublicationImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (file) {
      const validationError = this.validateImageFile(file);
      if (validationError) {
        this.publicationForm.image = null;
        input.value = '';
        this.publicationFeedback = { type: 'error', text: validationError };
        return;
      }
    }
    this.publicationForm.image = file;
  }

  onImportImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (file) {
      const validationError = this.validateImageFile(file);
      if (validationError) {
        this.importForm.image = null;
        input.value = '';
        this.importFeedback = { type: 'error', text: validationError };
        return;
      }
    }
    this.importForm.image = file;
  }

  importDocument(): void {
    if (!this.session) {
      this.importFeedback = { type: 'error', text: this.t('loginRequiredPublication') };
      return;
    }
    if (!this.importForm.file) {
      this.importFeedback = { type: 'error', text: this.t('invalidForm') };
      return;
    }

    this.importing = true;
    this.importFeedback = null;
    this.api.importDocument(this.importForm.file, this.importForm.visibility, this.importForm.image).subscribe({
      next: (publication) => {
        this.importing = false;
        this.importForm = { file: null, image: null, visibility: 'INSTITUTION' };
        this.importFeedback = null;
        this.page = 'validation';
        this.message = this.t('importCompleted') + ' ' + this.t('importQueued');
        this.loadPublications(false);
        this.loadValidationQueue();
        this.refreshImportStatus(publication.id);
      },
      error: (err) => {
        this.importing = false;
        this.importFeedback = { type: 'error', text: this.describeError(err, 'importFailed') };
      }
    });
  }

  private validateDocumentFile(file: File): string | null {
    const extension = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : '';
    if (!this.allowedFileExtensions.includes(extension)) {
      return this.t('unsupportedFileType');
    }
    if (file.size > this.maxFileSize) {
      return this.t('fileTooLarge');
    }
    return null;
  }

  private validateImageFile(file: File): string | null {
    if (!this.allowedImageTypes.includes(file.type)) {
      return this.t('unsupportedImageType');
    }
    if (file.size > this.maxImageSize) {
      return this.t('imageTooLarge');
    }
    return null;
  }

  private describeError(error: unknown, fallbackKey: TranslationKey): string {
    const httpError = error as { status?: number; error?: { message?: unknown } };
    if (httpError?.status === 0) {
      return this.t('apiUnavailable');
    }
    if (httpError?.status === 401) {
      return this.t('sessionExpired');
    }
    const serverMessage = httpError?.error?.message;
    if (typeof serverMessage === 'string' && serverMessage.trim().length > 0) {
      return serverMessage;
    }
    return this.t(fallbackKey);
  }

  private refreshImportStatus(publicationId: number, attempts = 0): void {
    if (attempts >= 30) {
      return;
    }
    window.setTimeout(() => {
      this.api.getPublication(publicationId).subscribe({
        next: (publication) => {
          const index = this.publications.findIndex((item) => item.id === publicationId);
          if (index >= 0) {
            this.publications[index] = publication;
          }
          const queueIndex = this.validationQueue.findIndex((item) => item.id === publicationId);
          if (queueIndex >= 0) {
            this.validationQueue[queueIndex] = publication;
          } else if (publication.status !== 'PUBLIE' && publication.status !== 'SUPPRIME') {
            this.validationQueue = [...this.validationQueue, publication];
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
      this.extractionQuality = null;
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
    this.loadExtractionQuality();
  }

  /** Fiabilite mesuree du LLM : calculee sur les arbitrages reels du bibliothecaire. */
  loadExtractionQuality(): void {
    if (!this.session) {
      this.extractionQuality = null;
      return;
    }

    this.api.getExtractionQuality().subscribe({
      next: (quality) => {
        this.extractionQuality = quality;
      },
      error: () => {
        this.extractionQuality = null;
      }
    });
  }

  /** Vocabulaires controles : le backend refuse tout code absent de ces listes. */
  loadReferences(): void {
    this.api.getReferences().subscribe({
      next: (references) => {
        this.references = references;
      },
      error: () => {
        this.references = { langues: [], types_documents: [] };
      }
    });
  }

  /** Libelle lisible d'un champ mesure, pour le tableau de qualite. */
  fieldLabel(field: string): string {
    const labels: Record<string, string> = {
      titre: this.t('metadataTitle'),
      resume: this.t('metadataSummary'),
      classification: this.t('metadataClassification'),
      auteurs: this.t('metadataAuthors'),
      mots_cles: this.t('metadataKeywords'),
      langue: this.t('metadataLanguage'),
      type_document: this.t('metadataDocumentType'),
      doi: this.t('metadataDoi'),
      date_publication: this.t('metadataPublicationDate')
    };
    return labels[field] || field;
  }

  purchaseCredits(packId: number): void {
    if (!this.session) {
      return;
    }

    if (!this.paymentTermsAccepted || !this.paymentWithdrawalWaiverAccepted) {
      this.message = this.t('paymentConsentRequired');
      return;
    }

    this.api.startCreditCheckout(packId, this.paymentTermsAccepted, this.paymentWithdrawalWaiverAccepted).subscribe({
      next: (checkout) => {
        window.location.assign(checkout.checkout_url);
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

    if (this.extractingPublicationIds.has(publication.id)) {
      return;
    }
    this.extractingPublicationIds.add(publication.id);
    this.message = this.t('processing');
    this.api.extractMetadata(publication.id).subscribe({
      next: (result) => {
        this.extractingPublicationIds.delete(publication.id);
        this.extractionResult = result;
        this.creditBalance = result.creditBalance;
        this.message = this.t('metadataLoaded');
        this.loadCreditMovements();
        this.loadStatistics();
        this.loadPublications();
        this.loadValidationQueue();
        this.page = 'validation';
        this.startMetadataValidation(publication);
      },
      error: (err) => {
        this.extractingPublicationIds.delete(publication.id);
        this.message = this.describeError(err, 'extractionFailed');
        this.loadValidationQueue();
      }
    });
  }

  retryDocumentProcessing(publication: Publication): void {
    this.message = this.t('processing');
    this.api.retryDocumentProcessing(publication.id).subscribe({
      next: (updated) => {
        const index = this.validationQueue.findIndex((item) => item.id === publication.id);
        if (index >= 0) {
          this.validationQueue[index] = updated;
        }
        this.refreshImportStatus(publication.id);
      },
      error: (err) => {
        this.message = this.describeError(err, 'extractionFailed');
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
          language: '',
          documentType: '',
          doi: '',
          extractedText: '',
          rejectReason: ''
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
        .filter(Boolean),
      langue: this.metadataForm.language || null,
      type_document: this.metadataForm.documentType || null,
      doi: this.metadataForm.doi.trim() || null
    }).subscribe({
      next: () => {
        this.cancelMetadataValidation();
        this.message = this.t('publicationPublished');
        this.loadStatistics();
        this.loadPublications(false);
        this.loadValidationQueue();
      },
      error: () => {
        this.message = this.t('statusUpdateFailed');
      }
    });
  }

  cancelMetadataValidation(): void {
    this.selectedMetadataPublicationId = null;
    this.showRejectForm = false;
    this.metadataForm = {
      documentId: 0,
      title: '',
      summary: '',
      publicationDate: '',
      classification: '',
      visibility: 'PUBLIC',
      authors: '',
      keywords: '',
      language: '',
      documentType: '',
      doi: '',
      extractedText: '',
      rejectReason: ''
    };
  }

  toggleRejectForm(): void {
    this.showRejectForm = !this.showRejectForm;
  }

  rejectMetadata(): void {
    if (!this.session || !this.selectedMetadataPublicationId) {
      return;
    }
    if (this.metadataForm.rejectReason.trim().length === 0) {
      this.message = this.t('rejectReasonRequired');
      return;
    }
    this.api.rejectMetadata(this.selectedMetadataPublicationId, this.metadataForm.rejectReason.trim()).subscribe({
      next: () => {
        this.cancelMetadataValidation();
        this.message = this.t('metadataRejected');
        this.loadStatistics();
        this.loadPublications(false);
        this.loadValidationQueue();
      },
      error: (err) => {
        this.message = this.describeError(err, 'statusUpdateFailed');
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
      next: () => {
        this.message = this.t('publicationDeleted');
        this.selectedMetadataPublicationId = null;
        this.loadStatistics();
        this.loadPublications(false);
        this.loadValidationQueue();
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
    this.extractionQuality = null;
    this.extractionResult = null;
    this.selectedPublication = null;
    this.deletionRequested = false;
    this.profileSaved = false;
    this.page = 'accueil';
  }

  exportPersonalData(): void {
    if (!this.session) {
      return;
    }
    this.api.exportPersonalData(this.session.id).subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'mes-donnees-metamind.json';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.message = this.t('apiUnavailable');
      }
    });
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

  activateUser(user: UserSession): void {
    if (!this.isAdmin) {
      return;
    }

    this.api.updateAdminUser(user.id, { role: user.role === 'ADMIN' ? 'ADMIN' : 'LIBRARIAN', statut: 'ACTIF' }).subscribe({
      next: () => {
        this.message = this.t('accountActivated');
        this.loadAdminData();
      },
      error: () => {
        this.message = this.t('statusUpdateFailed');
      }
    });
  }

  changeUserRole(user: UserSession, role: string): void {
    if (!this.isAdmin || (role !== 'ADMIN' && role !== 'LIBRARIAN') || role === user.role) {
      return;
    }
    const statut: 'EN_ATTENTE' | 'ACTIF' | 'DESACTIVE' =
      user.status === 'EN_ATTENTE' || user.status === 'DESACTIVE' ? user.status : 'ACTIF';
    this.api.updateAdminUser(user.id, { role: role as 'LIBRARIAN' | 'ADMIN', statut }).subscribe({
      next: () => {
        this.message = this.t('roleUpdated');
        this.loadAdminData();
      },
      error: () => {
        this.message = this.t('statusUpdateFailed');
        this.loadAdminData();
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
    return !!this.session && publication.institution === this.session.institution && publication.status === 'A_VALIDER';
  }

  canPublishPublication(publication: Publication): boolean {
    return this.canManagePublication(publication) && publication.status === 'A_VALIDER';
  }

  isExtracting(publicationId: number): boolean {
    return this.extractingPublicationIds.has(publicationId);
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
      language: metadata.langue || '',
      documentType: metadata.type_document || '',
      doi: metadata.doi || '',
      extractedText: metadata.texte_extrait || '',
      rejectReason: ''
    };
    this.selectedMetadataPublicationId = publication.id;
  }
}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiService, AuthResponse, CreditBalance, Publication } from './api.service';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let api: jasmine.SpyObj<ApiService>;

  const publications: Publication[] = [
    {
      id: 1,
      title: 'Analyse automatique des metadonnees',
      author: 'Sarah Lemaire',
      institution: 'Institution A',
      year: 2026,
      status: 'PUBLIE',
      visibility: 'PUBLIC',
      keywords: ['Dublin Core']
    }
  ];

  const authResponse: AuthResponse = {
    token: 'token-test',
    user: {
      id: 10,
      firstName: 'Sarah',
      lastName: 'Lemaire',
      email: 'sarah@institution-a.example',
      role: 'Bibliothecaire',
      institution: 'Institution A',
      status: 'ACTIF'
    }
  };

  beforeEach(async () => {
    api = jasmine.createSpyObj<ApiService>('ApiService', [
      'setToken',
      'getPublications',
      'createPublication',
      'importDocument',
      'getInstitutions',
      'createInstitution',
      'deactivateInstitution',
      'getCreditAccount',
      'getCreditPacks',
      'startCreditCheckout',
      'confirmCreditPayment',
      'getCreditBalance',
      'getCreditMovements',
      'getStatistics',
      'purchaseCredits',
      'extractMetadata',
      'getMetadata',
      'validateMetadata',
      'updatePublicationStatus',
      'deletePublication',
      'login',
      'register',
      'updateProfile',
      'requestAccountDeletion',
      'getAdminUsers',
      'updateAdminUser',
      'getAdminConfig',
      'getAdminLogs'
    ]);
    api.getPublications.and.returnValue(of(publications));
    api.getCreditPacks.and.returnValue(of([
      { id: 2, credits: 100, amount: 50, currency: 'EUR', label: 'Pack standard' }
    ]));
    api.getCreditAccount.and.returnValue(of({
      balance: { institutionId: 1, institution: 'Institution A', balance: 20 },
      movements: []
    }));
    api.getCreditBalance.and.returnValue(of({ institutionId: 1, institution: 'Institution A', balance: 20 }));
    api.getCreditMovements.and.returnValue(of([
      {
        id: 1,
        institution: 'Institution A',
        type: 'ACHAT',
        amount: 20,
        balanceAfter: 20,
        description: 'Achat de credits',
        createdAt: '2026-08-25T10:00:00'
      }
    ]));
    api.getStatistics.and.returnValue(of({
      scope: 'Institution A',
      totalPublications: 2,
      publishedPublications: 1,
      pendingValidationPublications: 1,
      publicPublications: 1,
      institutionOnlyPublications: 1,
      creditBalance: 20
    }));
    api.getAdminUsers.and.returnValue(of([]));
    api.getAdminConfig.and.returnValue(of({ prix_credit_eur: '0.50' }));
    api.getAdminLogs.and.returnValue(of([]));
    api.getMetadata.and.returnValue(of({
      id: 7,
      document_id: 1,
      titre: 'Analyse automatique des metadonnees',
      resume: 'Resume valide par un bibliothecaire.',
      date_publication: '2026-01-01',
      classification: 'Sciences de l information',
      visibilite: 'PUBLIC',
      statut: 'EN_ATTENTE',
      date_validation: null,
      validee_par: null,
      auteurs: [{ nom_complet: 'Sarah Lemaire' }],
      mots_cles: ['Dublin Core']
    }));

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: ApiService, useValue: api }]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('charge le catalogue au demarrage', () => {
    expect(api.getPublications).toHaveBeenCalled();
    expect(component.publications).toEqual(publications);
  });

  it('change la langue de l interface', () => {
    component.setLanguage('nl');

    expect(component.t('catalogue')).toBe('Catalogus');

    component.setLanguage('en');

    expect(component.t('catalogue')).toBe('Catalogue');
    expect(component.t('login')).toBe('Sign in');
  });

  it('connecte un bibliothecaire et charge son solde de credits', () => {
    api.login.and.returnValue(of(authResponse));

    component.login();

    expect(api.setToken).toHaveBeenCalledWith('token-test');
    expect(component.session?.email).toBe('sarah@institution-a.example');
    expect(component.page).toBe('profil');
    expect(component.creditBalance).toBe(20);
    expect(component.statistics?.scope).toBe('Institution A');
    expect(component.creditMovements.length).toBe(1);
  });

  it('charge les statistiques du tableau de bord', () => {
    component.session = authResponse.user;

    component.loadStatistics();

    expect(api.getStatistics).toHaveBeenCalled();
    expect(component.statistics?.totalPublications).toBe(2);
    expect(component.creditBalance).toBe(20);
  });

  it('met a jour le solde apres un achat de credits', () => {
    const balance: CreditBalance = { institutionId: 1, institution: 'Institution A', balance: 30 };
    api.startCreditCheckout.and.returnValue(of({ checkout_url: '/paiement/confirmation', reference: 'pay_123' }));
    api.confirmCreditPayment.and.returnValue(of(balance));
    api.getStatistics.and.returnValue(of({
      scope: 'Institution A',
      totalPublications: 2,
      publishedPublications: 1,
      pendingValidationPublications: 1,
      publicPublications: 1,
      institutionOnlyPublications: 1,
      creditBalance: 30
    }));
    component.session = authResponse.user;

    component.purchaseCredits(2);

    expect(api.startCreditCheckout).toHaveBeenCalledWith(2);
    expect(api.confirmCreditPayment).toHaveBeenCalledWith('pay_123');
    expect(api.getCreditMovements).toHaveBeenCalledWith(10);
    expect(component.creditBalance).toBe(30);
  });

  it('refuse de creer une publication incomplete', () => {
    component.session = authResponse.user;
    component.publicationForm = {
      title: '',
      author: 'A',
      year: 2026,
      visibility: 'PUBLIC',
      keywords: ''
    };

    component.createPublication();

    expect(api.createPublication).not.toHaveBeenCalled();
    expect(component.message).toBe(component.t('invalidForm'));
  });

  it('valide une publication complete', () => {
    component.publicationForm = {
      title: 'Controle qualite des metadonnees',
      author: 'Mina Laurent',
      year: 2026,
      visibility: 'INSTITUTION',
      keywords: 'qualite, catalogage'
    };

    expect(component.isPublicationFormValid()).toBeTrue();
  });

  it('publie une publication de la meme institution', () => {
    api.updatePublicationStatus.and.returnValue(of({ ...publications[0], status: 'PUBLIE' }));
    component.session = authResponse.user;
    component.publications = [{ ...publications[0], status: 'A_VALIDER' }];

    component.updatePublicationStatus(component.publications[0], 'PUBLIE');

    expect(api.updatePublicationStatus).toHaveBeenCalledWith(1, { status: 'PUBLIE' });
    expect(component.message).toBe(component.t('publicationPublished'));
  });

  it('prepare la validation des metadonnees', () => {
    component.session = authResponse.user;

    component.startMetadataValidation(publications[0]);

    expect(api.getMetadata).toHaveBeenCalledWith(1);
    expect(component.selectedMetadataPublicationId).toBe(1);
    expect(component.metadataForm.title).toBe('Analyse automatique des metadonnees');
    expect(component.metadataForm.authors).toBe('Sarah Lemaire');
  });

  it('valide les metadonnees corrigees', () => {
    api.validateMetadata.and.returnValue(of({
      id: 7,
      document_id: 1,
      titre: 'Analyse automatique des metadonnees',
      resume: 'Resume valide par un bibliothecaire.',
      date_publication: '2026-01-01',
      classification: 'Sciences de l information',
      visibilite: 'PUBLIC',
      statut: 'VALIDE',
      date_validation: '2026-09-05T12:00:00',
      validee_par: 10,
      auteurs: [{ nom_complet: 'Sarah Lemaire' }],
      mots_cles: ['Dublin Core']
    }));
    component.session = authResponse.user;
    component.selectedMetadataPublicationId = 1;
    component.metadataForm = {
      documentId: 1,
      title: 'Analyse automatique des metadonnees',
      summary: 'Resume valide par un bibliothecaire.',
      publicationDate: '2026-01-01',
      classification: 'Sciences de l information',
      visibility: 'PUBLIC',
      authors: 'Sarah Lemaire',
      keywords: 'Dublin Core'
    };

    component.validateMetadata();

    expect(api.validateMetadata).toHaveBeenCalledWith(1, jasmine.objectContaining({
      titre: 'Analyse automatique des metadonnees',
      visibilite: 'PUBLIC',
      auteurs: [{ nom_complet: 'Sarah Lemaire' }],
      mots_cles: ['Dublin Core']
    }));
    expect(component.selectedMetadataPublicationId).toBeNull();
    expect(component.message).toBe(component.t('publicationPublished'));
  });

  it('enregistre une suppression logique de publication', () => {
    api.deletePublication.and.returnValue(of({ ...publications[0], status: 'SUPPRIME' }));
    component.session = authResponse.user;
    component.publications = publications;

    component.deletePublication(publications[0]);

    expect(api.deletePublication).toHaveBeenCalledWith(1);
    expect(component.message).toBe(component.t('publicationDeleted'));
  });

  it('refuse de gerer une publication d une autre institution', () => {
    component.session = authResponse.user;
    const publication = { ...publications[0], institution: 'Institution B' };

    component.updatePublicationStatus(publication, 'PUBLIE');

    expect(api.updatePublicationStatus).not.toHaveBeenCalled();
    expect(component.message).toBe(component.t('statusUpdateFailed'));
  });

  it('limite l extraction aux publications de son institution', () => {
    component.session = authResponse.user;

    expect(component.canExtractPublication(publications[0])).toBeTrue();
    expect(component.canExtractPublication({ ...publications[0], institution: 'Institution B' })).toBeFalse();
  });

  it('refuse une inscription avec un mot de passe trop court', () => {
    component.registerForm = {
      firstName: 'Sarah',
      lastName: 'Lemaire',
      email: 'sarah@institution-a.example',
      institution: 'Institution A',
      password: '558435'
    };

    expect(component.isRegisterFormValid()).toBeFalse();
  });
});

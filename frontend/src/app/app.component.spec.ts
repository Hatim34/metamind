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
      'getInstitutions',
      'createInstitution',
      'deactivateInstitution',
      'getCreditBalance',
      'purchaseCredits',
      'extractMetadata',
      'login',
      'register',
      'updateProfile',
      'requestAccountDeletion'
    ]);
    api.getPublications.and.returnValue(of(publications));
    api.getCreditBalance.and.returnValue(of({ institutionId: 1, institution: 'Institution A', balance: 20 }));

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
  });

  it('connecte un bibliothecaire et charge son solde de credits', () => {
    api.login.and.returnValue(of(authResponse));

    component.login();

    expect(api.setToken).toHaveBeenCalledWith('token-test');
    expect(component.session?.email).toBe('sarah@institution-a.example');
    expect(component.page).toBe('profil');
    expect(component.creditBalance).toBe(20);
  });

  it('met a jour le solde apres un achat de credits', () => {
    const balance: CreditBalance = { institutionId: 1, institution: 'Institution A', balance: 30 };
    api.purchaseCredits.and.returnValue(of(balance));
    component.session = authResponse.user;

    component.purchaseCredits(10);

    expect(api.purchaseCredits).toHaveBeenCalledWith(10, 10);
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
});

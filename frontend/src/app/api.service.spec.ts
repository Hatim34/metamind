import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('charge les publications depuis l API versionnee', () => {
    service.getPublications().subscribe((publications) => {
      expect(publications).toEqual([]);
    });

    const request = httpMock.expectOne('/api/v1/publications');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('ajoute le filtre de recherche au catalogue', () => {
    service.getPublications('Dublin Core').subscribe();

    const request = httpMock.expectOne('/api/v1/publications?search=Dublin%20Core');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('lit la fiche complete d une publication', () => {
    service.getPublication(5).subscribe((publication) => {
      expect(publication.summary).toBe('Resume valide par un bibliothecaire.');
      expect(publication.publicationDate).toBe('2026-01-01');
      expect(publication.classification).toBe('Sciences de l information');
      expect(publication.language).toBe('fr');
      expect(publication.documentType).toBe('Article scientifique');
      expect(publication.fileUrl).toBe('/api/v1/documents/5/file');
    });

    const request = httpMock.expectOne('/api/v1/publications/5');
    expect(request.request.method).toBe('GET');
    request.flush({
      id: 5,
      titre: 'Analyse automatique des metadonnees',
      auteur: 'Sarah Lemaire',
      institution: 'Institution A',
      annee: 2026,
      resume: 'Resume valide par un bibliothecaire.',
      date_publication: '2026-01-01',
      classification: 'Sciences de l information',
      langue: 'fr',
      type_document: 'Article scientifique',
      statut: 'PUBLIE',
      visibilite: 'PUBLIC',
      mots_cles: ['Dublin Core'],
      fichier_url: '/api/v1/documents/5/file'
    });
  });

  it('recherche les publications publiques avec filtres', () => {
    service.searchPublications('metadata', {
      author: 'Sarah',
      language: 'fr',
      documentType: 'article',
      startDate: '2024-01-01',
      endDate: '2026-12-31'
    }).subscribe((publications) => {
      expect(publications.length).toBe(1);
      expect(publications[0].title).toBe('Analyse automatique des metadonnees');
    });

    const request = httpMock.expectOne('/api/v1/search?q=metadata&author=Sarah&langue=fr&type=article&date_debut=2024-01-01&date_fin=2026-12-31');
    expect(request.request.method).toBe('GET');
    request.flush({
      contenu: [{
        id: 5,
        titre: 'Analyse automatique des metadonnees',
        auteur: 'Sarah Lemaire',
        institution: 'Institution A',
        annee: 2026,
        statut: 'PUBLIE',
        visibilite: 'PUBLIC',
        mots_cles: ['Dublin Core']
      }],
      page: 0,
      size: 20,
      total_elements: 1,
      total_pages: 1
    });
  });

  it('envoie le jeton JWT sur les routes protegees', () => {
    service.setToken('token-test');

    service.getCreditBalance(10).subscribe();

    const request = httpMock.expectOne('/api/v1/users/10/credits');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({ institution_id: 1, institution: 'Institution A', solde_credits: 20 });
  });

  it('achete des credits via l API', () => {
    service.setToken('token-test');

    service.startCreditCheckout(2).subscribe((response) => {
      expect(response.reference).toBe('pay_123');
    });

    const request = httpMock.expectOne('/api/v1/credits');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ pack_id: 2, cgv_acceptees: true });
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({ checkout_url: '/paiement/confirmation', reference: 'pay_123' });
  });

  it('confirme un paiement de credits', () => {
    service.confirmCreditPayment('pay_123').subscribe((response) => {
      expect(response.balance).toBe(100);
    });

    const request = httpMock.expectOne('/api/v1/webhooks/stripe');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ reference: 'pay_123', type: 'checkout.session.completed' });
    request.flush({ institution_id: 1, institution: 'Institution A', solde_credits: 100 });
  });

  it('charge l historique des credits via l API', () => {
    service.setToken('token-test');

    service.getCreditMovements(10).subscribe((response) => {
      expect(response.length).toBe(1);
      expect(response[0].type).toBe('ACHAT');
    });

    const request = httpMock.expectOne('/api/v1/users/10/credits/movements');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush([
      {
        id: 1,
        institution: 'Institution A',
        type: 'ACHAT',
        montant: 10,
        solde_apres: 30,
        description: 'Achat de credits',
        date_creation: '2026-08-25T10:00:00'
      }
    ]);
  });

  it('charge les statistiques du tableau de bord', () => {
    service.setToken('token-test');

    service.getStatistics().subscribe((response) => {
      expect(response.scope).toBe('Institution A');
      expect(response.totalPublications).toBe(2);
    });

    const request = httpMock.expectOne('/api/v1/stats');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({
      scope: 'Institution A',
      total_publications: 2,
      publications_publiees: 1,
      publications_a_valider: 1,
      publications_publiques: 1,
      publications_institution: 1,
      solde_credits: 20
    });
  });

  it('demande une extraction de metadonnees', () => {
    service.setToken('token-test');

    service.extractMetadata(5).subscribe((response) => {
      expect(response.creditBalance).toBe(19);
    });

    const request = httpMock.expectOne('/api/v1/publications/5/extraction');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({
      publication_id: 5,
      titre: 'Analyse automatique des metadonnees',
      titre_suggere: 'Analyse automatique des metadonnees',
      auteur_suggere: 'Sarah Lemaire',
      mots_cles_suggeres: ['Dublin Core'],
      solde_credits: 19
    });
  });

  it('lit les metadonnees d un document', () => {
    service.setToken('token-test');

    service.getMetadata(5).subscribe((response) => {
      expect(response.titre).toBe('Analyse automatique des metadonnees');
    });

    const request = httpMock.expectOne('/api/v1/documents/5/metadata');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({
      id: 7,
      document_id: 5,
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
    });
  });

  it('valide les metadonnees d un document', () => {
    service.setToken('token-test');

    service.validateMetadata(5, {
      titre: 'Analyse automatique des metadonnees',
      resume: 'Resume valide par un bibliothecaire.',
      date_publication: '2026-01-01',
      classification: 'Sciences de l information',
      visibilite: 'PUBLIC',
      auteurs: [{ nom_complet: 'Sarah Lemaire' }],
      mots_cles: ['Dublin Core']
    }).subscribe((response) => {
      expect(response.statut).toBe('VALIDE');
    });

    const request = httpMock.expectOne('/api/v1/documents/5/metadata');
    expect(request.request.method).toBe('PUT');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    expect(request.request.body.titre).toBe('Analyse automatique des metadonnees');
    expect(request.request.body.visibilite).toBe('PUBLIC');
    request.flush({
      id: 7,
      document_id: 5,
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
    });
  });

  it('modifie le statut d une publication', () => {
    service.setToken('token-test');

    service.updatePublicationStatus(5, { status: 'PUBLIE' }).subscribe((response) => {
      expect(response.status).toBe('PUBLIE');
    });

    const request = httpMock.expectOne('/api/v1/publications/5/status');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ status: 'PUBLIE' });
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({
      id: 5,
      titre: 'Analyse automatique des metadonnees',
      auteur: 'Sarah Lemaire',
      institution: 'Institution A',
      annee: 2026,
      statut: 'PUBLIE',
      visibilite: 'PUBLIC',
      mots_cles: ['Dublin Core']
    });
  });

  it('lit les utilisateurs admin depuis une page', () => {
    service.setToken('token-test');

    service.getAdminUsers().subscribe((response) => {
      expect(response.length).toBe(1);
      expect(response[0].email).toBe('sarah@institution-a.example');
    });

    const request = httpMock.expectOne('/api/v1/admin/users');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({
      contenu: [{
        id: 10,
        prenom: 'Sarah',
        nom: 'Lemaire',
        email: 'sarah@institution-a.example',
        role: 'LIBRARIAN',
        institution: 'Institution A',
        statut: 'ACTIF'
      }],
      page: 0,
      size: 20,
      total_elements: 1,
      total_pages: 1
    });
  });
});

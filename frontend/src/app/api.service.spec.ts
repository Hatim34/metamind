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

  it('envoie le jeton JWT sur les routes protegees', () => {
    service.setToken('token-test');

    service.getCreditBalance(10).subscribe();

    const request = httpMock.expectOne('/api/v1/users/10/credits');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({ institutionId: 1, institution: 'Institution A', balance: 20 });
  });

  it('achete des credits via l API', () => {
    service.setToken('token-test');

    service.purchaseCredits(10, 25).subscribe((response) => {
      expect(response.balance).toBe(25);
    });

    const request = httpMock.expectOne('/api/v1/users/10/credits/purchase');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ amount: 25 });
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({ institutionId: 1, institution: 'Institution A', balance: 25 });
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
        amount: 10,
        balanceAfter: 30,
        description: 'Achat de credits',
        createdAt: '2026-08-25T10:00:00'
      }
    ]);
  });

  it('charge les statistiques du tableau de bord', () => {
    service.setToken('token-test');

    service.getStatistics().subscribe((response) => {
      expect(response.scope).toBe('Institution A');
      expect(response.totalPublications).toBe(2);
    });

    const request = httpMock.expectOne('/api/v1/statistics');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-test');
    request.flush({
      scope: 'Institution A',
      totalPublications: 2,
      publishedPublications: 1,
      pendingValidationPublications: 1,
      publicPublications: 1,
      institutionOnlyPublications: 1,
      creditBalance: 20
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
      publicationId: 5,
      title: 'Analyse automatique des metadonnees',
      suggestedTitle: 'Analyse automatique des metadonnees',
      suggestedAuthor: 'Sarah Lemaire',
      suggestedKeywords: ['Dublin Core'],
      creditBalance: 19
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
      title: 'Analyse automatique des metadonnees',
      author: 'Sarah Lemaire',
      institution: 'Institution A',
      year: 2026,
      status: 'PUBLIE',
      visibility: 'PUBLIC',
      keywords: ['Dublin Core']
    });
  });
});

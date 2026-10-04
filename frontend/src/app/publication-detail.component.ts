import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';

import { ApiService, Publication } from './api.service';

export interface PublicationDetailLabels {
  publicationDetails: string;
  backToCatalogue: string;
  noSummary: string;
  publicationDate: string;
  language: string;
  documentType: string;
  classification: string;
  status: string;
  visibility: string;
  downloadFile: string;
  noFile: string;
  publicVisibility: string;
  institutionOnly: string;
}

@Component({
  selector: 'app-publication-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './publication-detail.component.html'
})
export class PublicationDetailComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) publication!: Publication;
  @Input({ required: true }) labels!: PublicationDetailLabels;

  @Output() back = new EventEmitter<void>();
  @Output() download = new EventEmitter<Publication>();

  coverUrl: string | null = null;
  private objectUrl: string | null = null;
  private loadedFor: number | null = null;

  constructor(private readonly api: ApiService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['publication'] && this.publication?.id !== this.loadedFor) {
      this.loadCover();
    }
  }

  ngOnDestroy(): void {
    this.revoke();
  }

  get documentType(): string {
    return this.publication.documentType || this.labels.documentType;
  }

  get publicationYear(): string {
    return this.publication.publicationDate || (this.publication.year > 0 ? String(this.publication.year) : '');
  }

  get coverClass(): string {
    const type = this.documentType.toLocaleLowerCase();
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

  get visibilityLabel(): string {
    return this.publication.visibility === 'PUBLIC' ? this.labels.publicVisibility : this.labels.institutionOnly;
  }

  private loadCover(): void {
    this.revoke();
    this.coverUrl = null;
    this.loadedFor = this.publication?.id ?? null;
    if (!this.publication?.imageUrl || !this.publication?.id) {
      return;
    }
    const requestedId = this.publication.id;
    this.api.loadCoverImage(requestedId).subscribe({
      next: (blob) => {
        if (this.loadedFor !== requestedId) {
          return;
        }
        this.objectUrl = URL.createObjectURL(blob);
        this.coverUrl = this.objectUrl;
      },
      error: () => {
        this.coverUrl = null;
      }
    });
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}

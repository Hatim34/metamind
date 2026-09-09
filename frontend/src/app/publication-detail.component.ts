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

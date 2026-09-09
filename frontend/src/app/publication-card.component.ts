import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';

import { ApiService, Publication } from './api.service';

export interface PublicationCardLabels {
  consult: string;
  extract: string;
  edit: string;
  delete: string;
  processing: string;
  retry: string;
}

@Component({
  selector: 'app-publication-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './publication-card.component.html'
})
export class PublicationCardComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) publication!: Publication;
  @Input({ required: true }) labels!: PublicationCardLabels;
  @Input() allowExtract = false;
  @Input() allowEdit = false;
  @Input() allowDelete = false;
  @Input() allowRetry = false;

  @Output() consult = new EventEmitter<Publication>();
  @Output() extract = new EventEmitter<Publication>();
  @Output() edit = new EventEmitter<Publication>();
  @Output() remove = new EventEmitter<Publication>();
  @Output() retry = new EventEmitter<Publication>();

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

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
  stepImported: string;
  stepExtraction: string;
  stepToValidate: string;
  stepPublished: string;
  confirmDelete: string;
  confirm: string;
  cancel: string;
  documentType: string;
}

interface WorkflowStep {
  label: string;
  state: 'done' | 'current' | 'todo';
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
  @Input() showWorkflow = false;

  @Output() consult = new EventEmitter<Publication>();
  @Output() extract = new EventEmitter<Publication>();
  @Output() edit = new EventEmitter<Publication>();
  @Output() remove = new EventEmitter<Publication>();
  @Output() retry = new EventEmitter<Publication>();

  coverUrl: string | null = null;
  confirmingDelete = false;
  private objectUrl: string | null = null;
  private loadedFor: number | null = null;

  constructor(private readonly api: ApiService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['publication'] && this.publication?.id !== this.loadedFor) {
      this.loadCover();
      this.confirmingDelete = false;
    }
  }

  ngOnDestroy(): void {
    this.revoke();
  }

  /** Le document a-t-il deja recu des metadonnees enrichies (extraction faite) ? */
  get isEnriched(): boolean {
    return !!(this.publication?.summary && this.publication.summary.trim().length > 0)
      || (this.publication?.keywords?.length ?? 0) > 0;
  }

  /** Ligne "Auteur · Institution · Annee" sans separateurs vides ni annee 0. */
  get metaLine(): string {
    const parts: string[] = [];
    if (this.publication?.author && this.publication.author.trim().length > 0) {
      parts.push(this.publication.author.trim());
    }
    if (this.publication?.institution) {
      parts.push(this.publication.institution);
    }
    const date = this.publication?.publicationDate;
    if (date) {
      parts.push(date);
    } else if (this.publication?.year && this.publication.year > 0) {
      parts.push(String(this.publication.year));
    }
    return parts.join(' · ');
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

  /** Pipeline visuel : Importe, Extraction IA, A valider, Publie. */
  get steps(): WorkflowStep[] {
    const order = ['EN_ATTENTE', 'EXTRACTION', 'A_VALIDER', 'PUBLIE'];
    const current = Math.max(0, order.indexOf(this.publication.status));
    return [
      this.labels.stepImported,
      this.labels.stepExtraction,
      this.labels.stepToValidate,
      this.labels.stepPublished
    ].map((label, index) => ({
      label,
      state: index < current ? 'done' : index === current ? 'current' : 'todo'
    }));
  }

  askDelete(): void {
    this.confirmingDelete = true;
  }

  cancelDelete(): void {
    this.confirmingDelete = false;
  }

  confirmDelete(): void {
    this.confirmingDelete = false;
    this.remove.emit(this.publication);
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

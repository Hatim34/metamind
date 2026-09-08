import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Publication } from './api.service';

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
export class PublicationDetailComponent {
  @Input({ required: true }) publication!: Publication;
  @Input({ required: true }) labels!: PublicationDetailLabels;

  @Output() back = new EventEmitter<void>();
}

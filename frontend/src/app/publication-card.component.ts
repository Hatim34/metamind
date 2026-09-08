import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Publication } from './api.service';

export interface PublicationCardLabels {
  consult: string;
  extract: string;
  edit: string;
  delete: string;
  processing: string;
}

@Component({
  selector: 'app-publication-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './publication-card.component.html'
})
export class PublicationCardComponent {
  @Input({ required: true }) publication!: Publication;
  @Input({ required: true }) labels!: PublicationCardLabels;
  @Input() allowExtract = false;
  @Input() allowEdit = false;
  @Input() allowDelete = false;

  @Output() consult = new EventEmitter<Publication>();
  @Output() extract = new EventEmitter<Publication>();
  @Output() edit = new EventEmitter<Publication>();
  @Output() remove = new EventEmitter<Publication>();
}

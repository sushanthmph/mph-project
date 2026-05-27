import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Location } from '@angular/common';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.css']
})
export class AboutComponent {
  features = [
    {
      icon: '📤',
      title: 'File Upload',
      description: 'Upload transaction files in TXT or CSV format with drag and drop support'
    },
    {
      icon: '📊',
      title: 'Real-time Processing',
      description: 'Track file processing status and view detailed statistics in real-time'
    },
    {
      icon: '🗄️',
      title: 'Archive Management',
      description: 'Efficiently manage and archive successfully processed transaction files'
    },
    {
      icon: '⚠️',
      title: 'Error Tracking',
      description: 'Comprehensive error logs for failed transactions with detailed insights'
    }
  ];

  constructor(private location: Location) {}

  goBack(): void {
    this.location.back();
  }
}
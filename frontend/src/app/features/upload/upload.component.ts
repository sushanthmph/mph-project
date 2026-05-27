import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileService } from '../../core/services/file.service';
import { ToastService } from '../../core/services/toast.service';
import { UploadResponse } from '../../core/models/file-metadata.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css']
})
export class UploadComponent {
  selectedFile: File | null = null;
  uploadResponse: UploadResponse | null = null;
  loading = false;
  isDragging = false;

  readonly maxFileSize = environment.fileUploadMaxSize;
  readonly allowedExtensions = environment.allowedFileExtensions;

  constructor(
    private fileService: FileService,
    private toastService: ToastService
  ) {}

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    this.validateAndSetFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    
    const file = event.dataTransfer?.files[0];
    if (file) {
      this.validateAndSetFile(file);
    }
  }

  validateAndSetFile(file: File): void {
    // Check file size
    if (file.size > this.maxFileSize) {
      this.toastService.error(`File size exceeds ${this.maxFileSize /1048576}MB limit`);
      return;
    }

    // Check file extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !this.allowedExtensions.includes(extension)) {
      this.toastService.error(`Only ${this.allowedExtensions.join(', ')} files are allowed`);
      return;
    }

    this.selectedFile = file;
    this.uploadResponse = null;
  }

  uploadFile(): void {
    if (!this.selectedFile) {
      this.toastService.warning('Please select a file first');
      return;
    }

    this.loading = true;
    this.fileService.uploadFile(this.selectedFile).subscribe({
      next: (response) => {
        this.loading = false;
        this.uploadResponse = response.data;
        this.toastService.success(response.message);
      },
      error: () => {
        this.loading = false;
        this.toastService.error('File upload failed');
      }
    });
  }

  resetUpload(): void {
    this.selectedFile = null;
    this.uploadResponse = null;
  }

  getFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 4194304) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / 4194304).toFixed(2) + ' MB';
  }
}
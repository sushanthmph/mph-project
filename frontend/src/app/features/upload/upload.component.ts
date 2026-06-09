import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FileService } from '../../core/services/file.service';
import { ToastService } from '../../core/services/toast.service';
import { UploadResponse, FileMetadata } from '../../core/models/file-metadata.model';
import { environment } from '../../../environments/environment';
import { interval, Subscription } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css']
})
export class UploadComponent implements OnDestroy {
  selectedFile: File | null = null;
  uploadResponse: UploadResponse | null = null;
  fileMetadata: FileMetadata | null = null;
  loading = false;
  isDragging = false;
  isProcessing = false;

  readonly maxFileSize = environment.fileUploadMaxSize;
  readonly allowedExtensions = environment.allowedFileExtensions;

  private pollingSubscription?: Subscription;

  constructor(
    private fileService: FileService,
    private toastService: ToastService
  ) {}

  ngOnDestroy(): void {
    this.stopPolling();
  }

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
    if (file.size > this.maxFileSize) {
      this.toastService.error(`File size exceeds ${this.maxFileSize / 1048576}MB limit`);
      return;
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !this.allowedExtensions.includes(extension)) {
      this.toastService.error(`Only ${this.allowedExtensions.join(', ')} files are allowed`);
      return;
    }

    this.selectedFile = file;
    this.uploadResponse = null;
    this.fileMetadata = null;
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

        
        if (response.data.status === 'PROCESSING') {
          this.isProcessing = true;
          this.startPollingStatus(response.data.fileId);
        } else {
          this.fileMetadata = this.convertUploadResponseToFileMetadata(response.data);
        }
      },
      error: () => {
        this.loading = false;
        this.toastService.error('File upload failed');
      }
    });
  }

  private startPollingStatus(fileId: number): void {
    this.stopPolling(); 

    
    this.pollingSubscription = interval(2000).pipe(
      switchMap(() => this.fileService.getFileStatus(fileId)),
      takeWhile(response => {
        const status = response.data.status;
        return status === 'PROCESSING'; 
      }, true) 
    ).subscribe({
      next: (response) => {
        const metadata = response.data;
        
        if (metadata.status !== 'PROCESSING') {
          
          this.isProcessing = false;
          this.fileMetadata = metadata;
          
          
          if (this.uploadResponse) {
            this.uploadResponse.status = metadata.status;
            this.uploadResponse.totalRecords = metadata.recordCount;
            this.uploadResponse.successCount = metadata.successCount;
            this.uploadResponse.errorCount = metadata.errorCount;
          }

          this.stopPolling();

          
          if (metadata.errorCount > 0) {
            this.toastService.warning(`File processed with ${metadata.errorCount} errors`);
          } else {
            this.toastService.success('File processed successfully!');
          }
        }
      },
      error: (error) => {
        console.error('Error polling file status:', error);
        this.isProcessing = false;
        this.stopPolling();
      }
    });
  }

  private stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = undefined;
    }
  }

  private convertUploadResponseToFileMetadata(upload: UploadResponse): FileMetadata {
    return {
      fileId: upload.fileId,
      fileName: upload.fileName,
      uploadTime: new Date().toISOString(),
      recordCount: upload.totalRecords || 0,
      successCount: upload.successCount || 0,
      errorCount: upload.errorCount || 0,
      status: upload.status
    };
  }

  resetUpload(): void {
    this.stopPolling();
    this.selectedFile = null;
    this.uploadResponse = null;
    this.fileMetadata = null;
    this.isProcessing = false;
  }

  getFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / 1048576).toFixed(2) + ' MB';
  }


  hasErrors(): boolean {
    return (this.uploadResponse?.errorCount || 0) > 0 || 
           (this.fileMetadata?.errorCount || 0) > 0;
  }

  
  getStatusDisplay(): string {
    if (this.isProcessing) {
      return 'PROCESSING';
    }
    
    const status = this.fileMetadata?.status || this.uploadResponse?.status || '';
    
    
    if (status === 'COMPLETED' && this.hasErrors()) {
      return 'PARTIALLY COMPLETED';
    }
    
    return status;
  }


  getErrorCount(): number {
    return this.fileMetadata?.errorCount || this.uploadResponse?.errorCount || 0;
  }

  
  getSuccessCount(): number {
    return this.fileMetadata?.successCount || this.uploadResponse?.successCount || 0;
  }

  
  getTotalRecords(): number {
    return this.fileMetadata?.recordCount || this.uploadResponse?.totalRecords || 0;
  }
}
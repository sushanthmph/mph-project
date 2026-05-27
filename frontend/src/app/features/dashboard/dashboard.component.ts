import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FileService } from '../../core/services/file.service';
import { ToastService } from '../../core/services/toast.service';
import { FileMetadata, SearchParams } from '../../core/models/file-metadata.model';
import { FileUploadStatus } from '../../core/models/enums';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  files: FileMetadata[] = [];
  filteredFiles: FileMetadata[] = [];
  loading = false;
  showSearchModal = false;

  // Search filters
  searchParams: SearchParams = {};
  searchType: 'fileId' | 'fileName' | 'dateRange' | 'status' = 'fileName';

  // For status dropdown
  statusOptions = Object.values(FileUploadStatus);

  // View file details
  selectedFile: FileMetadata | null = null;
  showDetailsModal = false;

  // Confirmation Modal
  showConfirmModal = false;
  confirmAction: 'archive' | 'delete' | null = null;
  confirmFile: FileMetadata | null = null;

  constructor(
    private fileService: FileService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadFiles();
  }

  loadFiles(): void {
    this.loading = true;
    this.fileService.searchFiles({}).subscribe({
      next: (response) => {
        this.loading = false;
        this.files = response.data.filter(f => f.status !== 'DELETED' && f.status !== 'ARCHIVED');
        this.files.sort((a,b)=>b.fileId-a.fileId);
        this.filteredFiles = [...this.files];
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to load files');
      }
    });
  }

  openSearchModal(): void {
    this.showSearchModal = true;
    this.resetSearch();
  }

  closeSearchModal(): void {
    this.showSearchModal = false;
    this.resetSearch();
  }

  resetSearch(): void {
    this.searchParams = {};
    this.searchType = 'fileName';
  }

  performSearch(): void {
    this.loading = true;
    
    const params: SearchParams = {};
    
    if (this.searchType === 'fileId' && this.searchParams.fileId) {
      params.fileId = this.searchParams.fileId;
    } else if (this.searchType === 'fileName' && this.searchParams.fileName) {
      params.fileName = this.searchParams.fileName;
    } else if (this.searchType === 'dateRange') {
      params.uploadDateFrom = this.searchParams.uploadDateFrom;
      params.uploadDateTo = this.searchParams.uploadDateTo;
    } else if (this.searchType === 'status' && this.searchParams.status) {
      params.status = this.searchParams.status;
    }

    this.fileService.searchFiles(params).subscribe({
      next: (response) => {
        this.loading = false;
        this.filteredFiles = response.data.filter(f => f.status !== 'DELETED' && f.status !== 'ARCHIVED');
        this.filteredFiles.sort((a,b)=>b.fileId-a.fileId);
        this.toastService.success(`Found ${this.filteredFiles.length} file(s)`);
        this.closeSearchModal();
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Search failed');
      }
    });
  }

  viewFile(file: FileMetadata): void {
    this.loading = true;
    this.fileService.getFileStatus(file.fileId).subscribe({
      next: (response) => {
        this.loading = false;
        this.selectedFile = response.data;
        this.showDetailsModal = true;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to load file details');
      }
    });
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedFile = null;
  }

  // Open confirmation modal instead of browser confirm
  openArchiveConfirm(file: FileMetadata): void {
    if (file.successCount !== file.recordCount) {
      this.toastService.warning('Only files with 100% successful transactions can be archived');
      return;
    }
    this.confirmFile = file;
    this.confirmAction = 'archive';
    this.showConfirmModal = true;
  }

  openDeleteConfirm(file: FileMetadata): void {
    if (file.status === 'ARCHIVED') {
      this.toastService.warning('Archived files cannot be deleted');
      return;
    }
    this.confirmFile = file;
    this.confirmAction = 'delete';
    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
    this.confirmAction = null;
    this.confirmFile = null;
  }

  confirmActionExecute(): void {
    if (!this.confirmFile || !this.confirmAction) return;

    if (this.confirmAction === 'archive') {
      this.archiveFile(this.confirmFile);
    } else if (this.confirmAction === 'delete') {
      this.deleteFile(this.confirmFile);
    }
    
    this.closeConfirmModal();
  }

  archiveFile(file: FileMetadata): void {
    this.loading = true;
    this.fileService.archiveFile(file.fileId).subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.success(response.message);
        this.loadFiles();
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to archive file');
      }
    });
  }

  deleteFile(file: FileMetadata): void {
    this.loading = true;
    this.fileService.deleteFile(file.fileId).subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.success(response.message);
        this.loadFiles();
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to delete file');
      }
    });
  }

  canArchive(file: FileMetadata): boolean {
    return file.successCount === file.recordCount && file.recordCount > 0 && file.status !== 'ARCHIVED';
  }

  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'COMPLETED': 'status-success',
      'PARTIALLY_COMPLETED': 'status-warning',
      'PROCESSING': 'status-info',
      'PENDING': 'status-secondary',
      'FAILED': 'status-danger',
      'ARCHIVED': 'status-archived',
      'DELETED': 'status-deleted'
    };
    return statusMap[status] || 'status-secondary';
  }

  getSuccessPercentage(file: FileMetadata): number {
    if (file.recordCount === 0) return 0;
    return Math.round((file.successCount / file.recordCount) * 100);
  }

  refreshData(): void {
    this.fileService.clearCache();
    this.loadFiles();
  }
}
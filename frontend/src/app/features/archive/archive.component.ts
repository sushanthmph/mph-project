import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileService } from '../../core/services/file.service';
import { ToastService } from '../../core/services/toast.service';
import { FileMetadata } from '../../core/models/file-metadata.model';

@Component({
  selector: 'app-archive',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './archive.component.html',
  styleUrls: ['./archive.component.css']
})
export class ArchiveComponent implements OnInit {
  archivedFiles: FileMetadata[] = [];
  loading = false;
  selectedFile: FileMetadata | null = null;
  showDetailsModal = false;

  showConfirmModal = false;
  confirmFile: FileMetadata | null = null;

  constructor(
    private fileService: FileService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadArchivedFiles();
  }

  loadArchivedFiles(): void {
    this.loading = true;
    this.fileService.getArchivedFiles().subscribe({
      next: (response) => {
        this.loading = false;
        this.archivedFiles = response.data;
        
        this.archivedFiles.sort((a, b) => b.fileId - a.fileId);
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to load archived files');
      }
    });
  }

  viewFile(file: FileMetadata): void {
    this.selectedFile = file;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedFile = null;
  }

  openUnarchiveConfirm(file: FileMetadata): void {
    this.confirmFile = file;
    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
    this.confirmFile = null;
  }

  confirmUnarchive(): void {
    if (!this.confirmFile) return;
    
    this.unarchiveFile(this.confirmFile);
    this.closeConfirmModal();
  }

  unarchiveFile(file: FileMetadata): void {
    this.loading = true;
    this.fileService.unarchiveFile(file.fileId).subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.success(response.message);
        this.loadArchivedFiles();
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to unarchive file');
      }
    });
  }
}
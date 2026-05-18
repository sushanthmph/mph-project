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
        this.toastService.success(`Loaded ${this.archivedFiles.length} archived file(s)`);
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

  unarchiveFile(file: FileMetadata): void {
    if (confirm(`Are you sure you want to unarchive "${file.fileName}"? It will be moved back to the dashboard.`)) {
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
}
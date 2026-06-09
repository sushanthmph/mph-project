import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FileService } from '../../core/services/file.service';
import { ToastService } from '../../core/services/toast.service';
import { ErrorResponse } from '../../core/models/error-response.model';
import { Paginator } from '../../core/models/pagination.model';
import { debounceTime, Subject } from 'rxjs';

@Component({
  selector: 'app-error-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './error-logs.component.html',
  styleUrls: ['./error-logs.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErrorLogsComponent implements OnInit {
  allErrors: ErrorResponse[] = [];
  displayedErrors: ErrorResponse[] = [];
  paginator = new Paginator<ErrorResponse>([10, 20, 50, 100]);
  
  loading = false;
  showSearchModal = false;

  searchParams: { fileId?: string; fileName?: string } = {};
  searchType: 'fileId' | 'fileName' = 'fileId';
  searchFileId: string = '';
  searchFileName: string = '';
  private searchSubject = new Subject<void>();

  sortField: keyof ErrorResponse = 'timestamp';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private fileService: FileService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef
  ) {
    this.searchSubject.pipe(debounceTime(300)).subscribe(() => {
      this.performSearchInternal();
    });
  }

  ngOnInit(): void {
    this.loadErrorLogs();
  }

  loadErrorLogs(): void {
    this.loading = true;
    this.cdr.markForCheck();

    this.fileService.getErrorLogs().subscribe({
      next: (response) => {
        this.loading = false;
        this.allErrors = response.data || [];
        this.applySort();
        this.paginator.setData(this.allErrors);
        this.updateDisplayedErrors();
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to load error logs');
        this.cdr.markForCheck();
      }
    });
  }

  updateDisplayedErrors(): void {
    this.displayedErrors = this.paginator.getCurrentPageData();
    this.cdr.markForCheck();
  }

  nextPage(): void {
    if (this.paginator.nextPage()) {
      this.updateDisplayedErrors();
    }
  }

  previousPage(): void {
    if (this.paginator.previousPage()) {
      this.updateDisplayedErrors();
    }
  }

  goToPage(page: number): void {
    this.paginator.goToPage(page);
    this.updateDisplayedErrors();
  }

  changePageSize(size: number): void {
    this.paginator.setPageSize(size);
    this.updateDisplayedErrors();
  }

  sortBy(field: keyof ErrorResponse): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.applySort();
    this.paginator.setData(this.allErrors);
    this.updateDisplayedErrors();
  }

  applySort(): void {
    this.allErrors.sort((a, b) => {
      const aValue = a[this.sortField];
      const bValue = b[this.sortField];
      
      let comparison = 0;
      
      if (aValue === null || aValue === undefined) return 1;
      if (bValue === null || bValue === undefined) return -1;
      
      if (aValue > bValue) comparison = 1;
      if (aValue < bValue) comparison = -1;
      
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  getSortIcon(field: keyof ErrorResponse): string {
    if (this.sortField !== field) return '⇅';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  openSearchModal(): void {
    this.showSearchModal = true;
    this.resetSearch();
  }

  closeSearchModal(): void {
    this.showSearchModal = false;
  }

  resetSearch(): void {
    this.searchFileId = '';
    this.searchFileName = '';
    this.searchType = 'fileId';
  }

  triggerSearch(): void {
    if (this.searchType === 'fileId') {
      this.searchParams.fileId = this.searchFileId;
      this.searchParams.fileName = undefined;
    } else {
      this.searchParams.fileName = this.searchFileName;
      this.searchParams.fileId = undefined;
    }
    this.searchSubject.next();
  }

  performSearchInternal(): void {
    const fileId = this.searchParams.fileId;
    const fileName = this.searchParams.fileName;

    if (!fileId && !fileName) {
      this.toastService.warning('Please enter a search value');
      return;
    }

    this.loading = true;
    this.cdr.markForCheck();

    this.fileService.getErrorLogs().subscribe({
      next: (response) => {
        let filtered: ErrorResponse[] = response.data || [];
        
        if (fileId) {
          filtered = filtered.filter(err => err.fileId === fileId);
        }
        
        if (fileName) {
          this.toastService.warning('File name search requires additional backend support');
        }
        
        this.loading = false;
        this.allErrors = filtered;
        this.applySort();
        this.paginator.setData(filtered);
        this.updateDisplayedErrors();
        this.toastService.success(`Found ${filtered.length} error(s)`);
        this.closeSearchModal();
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Search failed');
        this.cdr.markForCheck();
      }
    });
  }

  clearSearch(): void {
    this.searchParams = {};
    this.resetSearch();
    this.loadErrorLogs();
  }

  refreshData(): void {
    this.fileService.clearCache();
    this.searchParams = {};
    this.loadErrorLogs();
  }

  trackByErrorId(index: number, error: ErrorResponse): number {
    return error.errorId;
  }

  isSearchActive(): boolean {
    return !!this.searchParams.fileId || !!this.searchParams.fileName;
  }
}
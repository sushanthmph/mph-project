import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileService } from '../../core/services/file.service';
import { ToastService } from '../../core/services/toast.service';
import { ErrorResponse } from '../../core/models/error-response.model';

@Component({
  selector: 'app-error-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error-logs.component.html',
  styleUrls: ['./error-logs.component.css']
})
export class ErrorLogsComponent implements OnInit {
  errorLogs: ErrorResponse[] = [];
  loading = false;

  constructor(
    private fileService: FileService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadErrorLogs();
  }

  loadErrorLogs(): void {
    this.loading = true;
    this.fileService.getErrorLogs().subscribe({
      next: (response) => {
        this.loading = false;
        this.errorLogs = response.data;
        this.toastService.success(`Loaded ${this.errorLogs.length} error log(s)`);
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Failed to load error logs');
      }
    });
  }
}
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { FileMetadata, UploadResponse, SearchParams } from '../models/file-metadata.model';
import { ErrorResponse } from '../models/error-response.model';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private readonly API_URL = `${environment.apiBaseUrl}/Files`;

  constructor(private http: HttpClient) {}

  uploadFile(file: File): Observable<ApiResponse<UploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<UploadResponse>>(`${this.API_URL}/upload`, formData);
  }

  getFileStatus(fileId: number): Observable<ApiResponse<FileMetadata>> {
    return this.http.get<ApiResponse<FileMetadata>>(`${this.API_URL}/status/${fileId}`);
  }

  searchFiles(params: SearchParams): Observable<ApiResponse<FileMetadata[]>> {
    let httpParams = new HttpParams();
    
    if (params.fileId) httpParams = httpParams.set('fileId', params.fileId.toString());
    if (params.fileName) httpParams = httpParams.set('fileName', params.fileName);
    if (params.uploadDateFrom) httpParams = httpParams.set('uploadDateFrom', params.uploadDateFrom);
    if (params.uploadDateTo) httpParams = httpParams.set('uploadDateTo', params.uploadDateTo);
    if (params.status) httpParams = httpParams.set('status', params.status);

    return this.http.get<ApiResponse<FileMetadata[]>>(`${this.API_URL}/search`, { params: httpParams });
  }

  archiveFile(fileId: number): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.API_URL}/archive/${fileId}`, {});
  }

  deleteFile(fileId: number): Observable<ApiResponse<string>> {
    return this.http.delete<ApiResponse<string>>(`${this.API_URL}/transactions/${fileId}`);
  }

  getArchivedFiles(): Observable<ApiResponse<FileMetadata[]>> {
    return this.http.get<ApiResponse<FileMetadata[]>>(`${this.API_URL}/archive`);
  }

  unarchiveFile(fileId: number): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.API_URL}/unarchive/${fileId}`, {});
  }

  getErrorLogs(): Observable<ApiResponse<ErrorResponse[]>> {
    return this.http.get<ApiResponse<ErrorResponse[]>>(`${this.API_URL}/errors`);
  }
}
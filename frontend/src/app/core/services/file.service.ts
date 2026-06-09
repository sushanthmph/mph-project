import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { FileMetadata, UploadResponse, SearchParams } from '../models/file-metadata.model';
import { ErrorResponse } from '../models/error-response.model';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private readonly API_URL = `${environment.apiBaseUrl}/Files`;
  
  private cache = new Map<string, { data: any; timestamp: number }>();
  private readonly CACHE_DURATION = 30000;

  constructor(private http: HttpClient) {}

  uploadFile(file: File): Observable<ApiResponse<UploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    this.clearCache(); 
    return this.http.post<ApiResponse<UploadResponse>>(`${this.API_URL}/upload`, formData);
  }

  getFileStatus(fileId: number): Observable<ApiResponse<FileMetadata>> {
    const cacheKey = `file-status-${fileId}`;
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return of(cached);
    }

    return this.http.get<ApiResponse<FileMetadata>>(`${this.API_URL}/status/${fileId}`).pipe(
      tap(response => this.setCache(cacheKey, response)),
      shareReplay(1)
    );
  }

  searchFiles(params: SearchParams): Observable<ApiResponse<FileMetadata[]>> {
    const cacheKey = `search-${JSON.stringify(params)}`;
    
    if (Object.keys(params).length === 0) {
      const cached = this.getFromCache(cacheKey);
      if (cached) {
        console.log('✅ Using cached file list');
        return of(cached);
      }
    }

    let httpParams = new HttpParams();
    
    if (params.fileId) httpParams = httpParams.set('fileId', params.fileId.toString());
    if (params.fileName) httpParams = httpParams.set('fileName', params.fileName);
    if (params.uploadDateFrom) httpParams = httpParams.set('uploadDateFrom', params.uploadDateFrom);
    if (params.uploadDateTo) httpParams = httpParams.set('uploadDateTo', params.uploadDateTo);
    if (params.status) httpParams = httpParams.set('status', params.status);

    console.log('🔄 Fetching fresh file list from server');
    return this.http.get<ApiResponse<FileMetadata[]>>(`${this.API_URL}/search`, { params: httpParams }).pipe(
      tap(response => {
        if (Object.keys(params).length === 0) {
          this.setCache(cacheKey, response);
        }
      }),
      shareReplay(1)
    );
  }

  archiveFile(fileId: number): Observable<ApiResponse<string>> {
    this.clearCache();
    return this.http.post<ApiResponse<string>>(`${this.API_URL}/archive/${fileId}`, {});
  }

  deleteFile(fileId: number): Observable<ApiResponse<string>> {
    this.clearCache();
    return this.http.delete<ApiResponse<string>>(`${this.API_URL}/transactions/${fileId}`);
  }

  getArchivedFiles(): Observable<ApiResponse<FileMetadata[]>> {
    const cacheKey = 'archived-files';
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return of(cached);
    }

    return this.http.get<ApiResponse<FileMetadata[]>>(`${this.API_URL}/archive`).pipe(
      tap(response => this.setCache(cacheKey, response)),
      shareReplay(1)
    );
  }

  unarchiveFile(fileId: number): Observable<ApiResponse<string>> {
    this.clearCache();
    return this.http.post<ApiResponse<string>>(`${this.API_URL}/unarchive/${fileId}`, {});
  }

  getErrorLogs(): Observable<ApiResponse<ErrorResponse[]>> {
    const cacheKey = 'error-logs';
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return of(cached);
    }

    return this.http.get<ApiResponse<ErrorResponse[]>>(`${this.API_URL}/errors`).pipe(
      tap(response => this.setCache(cacheKey, response)),
      shareReplay(1)
    );
  }

  private getFromCache(key: string): any {
    const cached = this.cache.get(key);
    if (cached && (Date.now() - cached.timestamp) < this.CACHE_DURATION) {
      console.log('✅ Cache hit:', key);
      return cached.data;
    }
    return null;
  }

  private setCache(key: string, data: any): void {
    console.log('💾 Caching:', key);
    this.cache.set(key, { data, timestamp: Date.now() });
  }

  clearCache(): void {
    console.log('🗑️ Clearing all cache');
    this.cache.clear();
  }
}
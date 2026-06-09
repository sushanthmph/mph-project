export interface FileMetadata {
  fileId: number;
  fileName: string;
  uploadTime: string;
  recordCount: number;
  successCount: number;
  errorCount: number;
  status: string;
}

export interface UploadResponse {
  fileId: number;
  fileName: string;
  status: string;
  message: string;
  totalRecords: number;   
  successCount: number;    
  errorCount: number; 
}

export interface SearchParams {
  fileId?: number;
  fileName?: string;
  uploadDateFrom?: string;
  uploadDateTo?: string;
  status?: string;
}
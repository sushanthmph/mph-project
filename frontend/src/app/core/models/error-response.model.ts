export interface ErrorResponse {
  errorId: number;
  transactionId: string;
  accountNumber: string;
  errorField: string;
  errorMessage: string;
  status: string;
  fileId: string;
  timestamp: string;
}
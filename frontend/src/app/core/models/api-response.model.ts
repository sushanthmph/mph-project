export interface ApiResponse<T> {
  timeStamp: string;
  status: string;
  code: string;
  message: string;
  data: T;
}
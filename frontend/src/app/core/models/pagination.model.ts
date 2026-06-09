export interface PaginationConfig {
  currentPage: number;
  pageSize: number;
  totalItems: number;
  pageSizeOptions: number[];
}

export class Paginator<T> {
  private allData: T[] = [];
  public currentPage = 0;
  public pageSize = 20;
  public totalPages = 0;

  constructor(public pageSizeOptions: number[] = [10, 20, 50, 100]) {}

  setData(data: T[]): void {
    this.allData = data;
    this.totalPages = Math.ceil(data.length / this.pageSize);
  }

  getCurrentPageData(): T[] {
    const start = this.currentPage * this.pageSize;
    const end = start + this.pageSize;
    return this.allData.slice(start, end);
  }

  nextPage(): boolean {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      return true;
    }
    return false;
  }

  previousPage(): boolean {
    if (this.currentPage > 0) {
      this.currentPage--;
      return true;
    }
    return false;
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
    }
  }

  setPageSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.totalPages = Math.ceil(this.allData.length / this.pageSize);
  }

  get hasNext(): boolean {
    return this.currentPage < this.totalPages - 1;
  }

  get hasPrevious(): boolean {
    return this.currentPage > 0;
  }

  get totalItems(): number {
    return this.allData.length;
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = 5;
    let startPage = Math.max(0, this.currentPage - 2);
    let endPage = Math.min(this.totalPages - 1, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(0, endPage - maxPages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }
}
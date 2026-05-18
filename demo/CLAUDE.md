# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
ng serve          # Start dev server at http://localhost:4200
ng build          # Production build
ng build --watch --configuration development  # Watch mode
ng test           # Run unit tests via Karma
ng test --include='**/path/to/specific.spec.ts'  # Run a single test file
```

There is no `dev` script — use `ng serve` or `npm start`.

## Architecture

This is an **Angular 18 standalone-component** application for trade file processing. All components use the standalone API (no NgModules except `SharedModule`).

### Request / Response contract

Every API call returns `ApiResponse<T>` with fields `{ timeStamp, status, code, message, data }`. Check `response.status === 'SUCCESS'` (see `ApiStatus` enum) before consuming `response.data`. The `errorInterceptor` handles HTTP errors globally and shows toasts; individual components only need to handle the `error` callback to reset loading state.

### Auth flow

- JWT token stored in `localStorage` under key `auth_token` (configurable via `environment.tokenKey`).
- `AuthService` exposes `isAuthenticated$` (BehaviorSubject) for reactive auth state.
- `authInterceptor` attaches `Authorization: Bearer <token>` to every request except `/auth/login` and `/auth/register`.
- `authGuard` (functional `CanActivateFn`) protects all routes except `/login` and `/register`; on failure it redirects to `/login?returnUrl=<url>`.
- `errorInterceptor` auto-calls `authService.logout()` on 401 responses.

### Feature modules (lazy-loaded standalone components)

| Route | Component | Notes |
|---|---|---|
| `/dashboard` | `DashboardComponent` | Lists active files; excludes `DELETED`/`ARCHIVED` statuses |
| `/upload` | `UploadComponent` | Accepts `.txt`/`.csv` ≤ 10 MB; drag-and-drop supported |
| `/archive` | `ArchiveComponent` | Archived files view; supports unarchive |
| `/error-logs` | `ErrorLogsComponent` | Per-transaction error records |

### Core services

- **`FileService`** — all file operations via `/Files/*` endpoints: upload, status, search, archive, unarchive, delete, error logs.
- **`AuthService`** — login, register, logout, token management.
- **`ToastService`** — singleton BehaviorSubject; `error()` shows for 5 s, others for 3 s. Components inject it directly — no need to pass error messages manually when `errorInterceptor` already handles HTTP errors.

### Key business rules

- A file can only be archived when `successCount === recordCount` (100% success rate).
- Archived files cannot be deleted.
- `FileUploadStatus` enum: `PENDING`, `PROCESSING`, `COMPLETED`, `PARTIALLY_COMPLETED`, `FAILED`, `ARCHIVED`, `DELETED`.

### Environment config

`src/environments/environment.ts` (dev) and `environment.prod.ts` (prod). Update `apiBaseUrl` in prod before deploying — it currently points to a placeholder URL.

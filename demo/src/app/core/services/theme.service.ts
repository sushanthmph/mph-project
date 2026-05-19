import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map } from 'rxjs';

export type Theme = 'light' | 'dark' | 'black';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'tfp_theme';

  private _theme = new BehaviorSubject<Theme>('black');
  theme$ = this._theme.asObservable();
  isDark$: Observable<boolean> = this._theme.pipe(map(t => t !== 'light'));

  get isDark(): boolean { return this._theme.value !== 'light'; }
  get theme(): Theme { return this._theme.value; }

  init(): void {
    const saved = localStorage.getItem(this.STORAGE_KEY) as Theme | null;
    const valid: Theme[] = ['light', 'dark', 'black'];
    if (saved && valid.includes(saved)) {
      this.apply(saved);
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.apply(prefersDark ? 'black' : 'light');
    }
  }

  setTheme(theme: Theme): void { this.apply(theme); }

  toggle(): void {
    const order: Theme[] = ['black', 'dark', 'light'];
    const next = order[(order.indexOf(this._theme.value) + 1) % order.length];
    this.apply(next);
  }

  private apply(theme: Theme): void {
    this._theme.next(theme);
    const html = document.documentElement;
    html.classList.remove('light', 'dark', 'black');
    html.classList.add(theme);
    localStorage.setItem(this.STORAGE_KEY, theme);
  }
}

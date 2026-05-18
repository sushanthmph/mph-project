import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  isAuthenticated$!: Observable<boolean>;

  constructor(
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
  }

  logout(): void {
    this.authService.logout();
  }
}
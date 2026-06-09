import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { FileService } from '../../../core/services/file.service';
import { LoginRequest } from '../../../core/models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginData: LoginRequest = {
    username: '',
    password: ''
  };
  loading = false;

  constructor(
    private authService: AuthService,
    private fileService: FileService,
    private router: Router,
    private toastService: ToastService
  ) {}

  onSubmit(): void {
    if (!this.loginData.username || !this.loginData.password) {
      this.toastService.warning('Please enter both username and password');
      return;
    }

    // ✅ UPDATED: Validate username format (no spaces)
    if (!this.isValidUsername(this.loginData.username)) {
      this.toastService.error('Username cannot contain spaces');
      return;
    }

    this.loading = true;
    
    // Clear cache before login
    this.fileService.clearCache();
    
    this.authService.login(this.loginData).subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.success('Login successful!');
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;
        this.toastService.error('Login failed. Please check your credentials.');
      }
    });
  }

  private isValidUsername(username: string): boolean {
    const usernamePattern = /^\S+$/;
    return usernamePattern.test(username);
  }
}
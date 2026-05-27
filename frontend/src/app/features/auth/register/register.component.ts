import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { RegisterRequest } from '../../../core/models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  registerData: RegisterRequest = {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
    password: ''
  };
  confirmPassword = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {}

  onSubmit(): void {
    if (this.registerData.password !== this.confirmPassword) {
      this.toastService.warning('Passwords do not match');
      return;
    }

    this.loading = true;
    this.authService.register(this.registerData).subscribe({
      next: (response) => {
        this.loading = false;
        this.toastService.success('Registration successful! Please login.');
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.loading = false;
        this.toastService.error('Registration failed. Please try again.');
      }
    });
  }

  // ── Password rule helpers ──────────────────────────────────────────────────
  hasUppercase(value: string): boolean {
    return /[A-Z]/.test(value || '');
  }

  hasLowercase(value: string): boolean {
    return /[a-z]/.test(value || '');
  }

  hasNumber(value: string): boolean {
    return /\d/.test(value || '');
  }

  hasSpecialChar(value: string): boolean {
    return /[!@._]/.test(value || '');
  }

  hasMinLength(value: string): boolean {
    return (value || '').length >= 8;
  }
}
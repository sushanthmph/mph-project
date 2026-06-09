import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { RegisterRequest } from '../../../core/models/user.model';
import { HttpErrorResponse } from '@angular/common/http';

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


  if (!this.isValidName(this.registerData.firstName)) {
    this.toastService.warning('First name can only contain letters and spaces');
    return;
  }
  if (this.registerData.firstName.length > 20) {
      this.toastService.warning('First name cannot exceed 20 characters');
      return;
    }

  if (!this.isValidName(this.registerData.lastName)) {
    this.toastService.warning('Last name can only contain letters and spaces');
    return;
  }
  if (this.registerData.lastName.length > 10) {
      this.toastService.warning('Last name cannot exceed 10 characters');
      return;
    }

  if (!this.isValidUsername(this.registerData.username)) {
    this.toastService.warning('Username cannot contain spaces');
    return;
  }

  this.loading = true;
  this.authService.register(this.registerData).subscribe({
    next: (response) => {
      this.loading = false;
      this.toastService.success('Registration successful! Please login.');
      this.router.navigate(['/login']);
    },
    error: (error: HttpErrorResponse) => {
      this.loading = false;
      this.handleRegistrationError(error);
    }
  });
}

private isValidName(name: string): boolean {
  const namePattern = /^[a-zA-Z\s]+$/;
  return namePattern.test(name);
}

private isValidUsername(username: string): boolean {
  const usernamePattern = /^\S+$/;
  return usernamePattern.test(username);
}
  private handleRegistrationError(error: HttpErrorResponse): void {
    console.error('Registration error:', error);

    // Check if error has a response body with message
    if (error.error && error.error.message) {
      const errorMessage = error.error.message.toLowerCase();

      // Check for specific error messages
      if (errorMessage.includes('email') && 
          (errorMessage.includes('already') || errorMessage.includes('exists') || errorMessage.includes('taken'))) {
        this.toastService.error('Email is already registered. Please use a different email or login.');
        return;
      }

      if (errorMessage.includes('username') && 
          (errorMessage.includes('already') || errorMessage.includes('exists') || errorMessage.includes('taken'))) {
        this.toastService.error('Username is already taken. Please choose a different username.');
        return;
      }

      
      this.toastService.error(error.error.message);
    } else if (error.status === 409) {
      
      this.toastService.error('Email or username already exists. Please use different credentials.');
    } else if (error.status === 400) {
      
      this.toastService.error('Invalid registration data. Please check your information.');
    } else if (error.status === 0) {
      
      this.toastService.error('Unable to connect to server. Please check your internet connection.');
    } else {
      this.toastService.error('Registration failed. Please try again later.');
    }
  }

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
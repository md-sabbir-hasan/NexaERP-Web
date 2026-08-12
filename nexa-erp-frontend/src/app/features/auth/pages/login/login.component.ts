import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { AlertService } from '../../../../core/services/alert.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly currentYear = new Date().getFullYear();
  readonly loginForm;

  constructor(
    private readonly fb: NonNullableFormBuilder,
    private readonly authService: AuthService,
    private readonly alert: AlertService,
    private readonly router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
      rememberMe: [false],
    });
  }

  submit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const { rememberMe, ...credentials } = this.loginForm.getRawValue();
    this.authService.login(credentials).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading.set(false);
        this.alert.error(error?.error?.message ?? 'Login failed');
      },
    });
  }

  togglePassword(): void {
    this.showPassword.update((value) => !value);
  }
}
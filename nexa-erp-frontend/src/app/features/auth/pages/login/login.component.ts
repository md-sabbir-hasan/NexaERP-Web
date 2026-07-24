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
    private fb: NonNullableFormBuilder,
    private authService: AuthService,
    private alert: AlertService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });
  }

  submit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);

    const request = this.loginForm.getRawValue();

    this.authService.login(request).subscribe({
      next: () => {
        this.authService.refreshCurrentUser().subscribe({
          next: () => {
            this.loading.set(false);
            this.router.navigate(['/dashboard']);
          },
          error: () => {
            // Profile fetch failed but login itself succeeded — still proceed
            this.loading.set(false);
            this.router.navigate(['/dashboard']);
          },
        });
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

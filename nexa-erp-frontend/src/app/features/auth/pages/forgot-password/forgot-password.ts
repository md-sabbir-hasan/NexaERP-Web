import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { AlertService } from '../../../../core/services/alert.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
})
export class ForgotPassword {
  readonly form;

  loading = false;

  constructor(
    private fb: NonNullableFormBuilder,
    private authService: AuthService,
    private alert: AlertService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.authService.forgotPassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading = false;

        this.alert.success(
          'Password reset link has been sent to your email.',
        );

        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.loading = false;

        this.alert.error(
          error?.error?.message ?? 'Failed to send password reset email.',
        );
      },
    });
  }
}
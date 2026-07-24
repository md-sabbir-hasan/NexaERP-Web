import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { AlertService } from '../../../../core/services/alert.service';

function passwordMatchValidator(
  control: AbstractControl,
): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword implements OnInit {
  readonly form;

  loading = false;

  private token = '';

  constructor(
    private fb: NonNullableFormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private alert: AlertService,
  ) {
    this.form = this.fb.group(
      {
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      {
        validators: passwordMatchValidator,
      },
    );
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.alert.error('Invalid password reset link.');
      this.router.navigate(['/forgot-password']);
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.authService
  .resetPassword({
    token: this.token,
    newPassword: this.form.controls.password.value,
    confirmPassword: this.form.controls.confirmPassword.value,
  })
      .subscribe({
        next: () => {
          this.loading = false;

          this.alert.success(
            'Password reset successfully. Please login.',
          );

          this.router.navigate(['/login']);
        },
        error: (error) => {
          this.loading = false;

          this.alert.error(
            error?.error?.message ?? 'Password reset failed.',
          );
        },
      });
  }
}
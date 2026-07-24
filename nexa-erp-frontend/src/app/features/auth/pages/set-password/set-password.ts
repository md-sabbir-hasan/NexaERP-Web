import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { AlertService } from '../../../../core/services/alert.service';

@Component({
  selector: 'app-set-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './set-password.html',
  styleUrl: './set-password.scss',
})
export class SetPassword implements OnInit {
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly invalidToken = signal(false);

  private inviteToken = '';

  readonly form;

  constructor(
    private fb: NonNullableFormBuilder,
    private route: ActivatedRoute,
    private authService: AuthService,
    private alert: AlertService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.inviteToken = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.inviteToken) {
      this.invalidToken.set(true);
      this.loading.set(false);
      return;
    }

    this.authService.validateInvite(this.inviteToken).subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.invalidToken.set(true);
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (value.password !== value.confirmPassword) {
      this.alert.error('Passwords do not match');
      return;
    }

    this.submitting.set(true);

    this.authService
      .setPassword({
        inviteToken: this.inviteToken,
        password: value.password,
        confirmPassword: value.confirmPassword,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.alert.success('Password set successfully. Please login.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.submitting.set(false);
          this.alert.error(err?.error?.message ?? 'Failed to set password');
        },
      });
  }
}

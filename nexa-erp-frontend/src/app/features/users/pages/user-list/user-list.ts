import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { Role } from '../../../roles/models/role.model';
import { RoleService } from '../../../roles/services/role.service';
import { User, UserStatus } from '../../models/user.model';
import { UserService } from '../../services/user.service';

function requiredArray(control: AbstractControl): ValidationErrors | null {
  const value = control.value as number[];
  return value && value.length > 0 ? null : { required: true };
}

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DatePipe],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserList implements OnInit {
  readonly users = signal<User[]>([]);
  readonly roles = signal<Role[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showUserModal = signal(false);

  selectedUser: User | null = null;

  search = '';
  status: UserStatus | '' = '';

  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  readonly userForm;

  constructor(
    private userService: UserService,
    private roleService: RoleService,
    private alert: AlertService,
    private fb: NonNullableFormBuilder,
  ) {
    this.userForm = this.fb.group({
      name: this.fb.control('', [Validators.required]),
      email: this.fb.control('', [Validators.required, Validators.email]),
      roleIds: this.fb.control<number[]>([], [requiredArray]),
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();
  }

  loadUsers(): void {
    this.loading.set(true);

    this.userService
      .getUsers(this.page, this.size, this.search, this.status || undefined)
      .subscribe({
        next: (res) => {
          this.users.set(res.data.content);
          this.page = res.data.page;
          this.size = res.data.size;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  loadRoles(): void {
    this.roleService.getAll().subscribe({
      next: (res) => {
        this.roles.set(res.data.filter((role) => role.name.toUpperCase() !== 'SUPER_ADMIN'));
      },
    });
  }

  onSearch(): void {
    this.page = 0;
    this.loadUsers();
  }

  clearSearch(): void {
    this.search = '';
    this.status = '';
    this.page = 0;
    this.loadUsers();
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.page++;
      this.loadUsers();
    }
  }

  previousPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadUsers();
    }
  }

  openInviteModal(): void {
    this.selectedUser = null;
    this.userForm.reset();
    this.userForm.patchValue({ roleIds: [] });
    this.showUserModal.set(true);
  }

  openEditModal(user: User): void {
    if (this.isSuperAdmin(user)) {
      this.alert.warning('SUPER_ADMIN user cannot be updated');
      return;
    }

    this.selectedUser = user;

    const selectedRoleIds = this.roles()
      .filter((role) => user.roles.includes(role.name))
      .map((role) => role.id);

    this.userForm.reset();
    this.userForm.patchValue({
      name: user.name,
      email: user.email,
      roleIds: selectedRoleIds,
    });

    this.showUserModal.set(true);
  }

  closeUserModal(): void {
    this.showUserModal.set(false);
    this.selectedUser = null;
  }

  submitUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const request = this.userForm.getRawValue();

    const apiCall = this.selectedUser
      ? this.userService.update(this.selectedUser.id, request)
      : this.userService.create(request);

    apiCall.subscribe({
      next: () => {
        this.submitting.set(false);
        this.showUserModal.set(false);
        this.alert.success(
          this.selectedUser ? 'User updated successfully' : 'User invited successfully',
        );
        this.selectedUser = null;
        this.loadUsers();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save user');
      },
    });
  }

  async deactivateUser(user: User): Promise<void> {
    const confirmed = await this.alert.confirm(`Are you sure you want to deactivate ${user.name}?`);

    if (!confirmed) return;

    this.userService.deactivate(user.id).subscribe({
      next: () => {
        this.alert.success('User deactivated successfully');
        this.loadUsers();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to deactivate user');
      },
    });
  }

  async activateUser(user: User): Promise<void> {
    const confirmed = await this.alert.confirm(`Are you sure you want to activate ${user.name}?`);

    if (!confirmed) return;

    this.userService.activate(user.id).subscribe({
      next: () => {
        this.alert.success('User activated successfully');
        this.loadUsers();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to activate user');
      },
    });
  }

  isSuperAdmin(user: User): boolean {
    return user.roles.some((role) => role.toUpperCase() === 'SUPER_ADMIN');
  }
}

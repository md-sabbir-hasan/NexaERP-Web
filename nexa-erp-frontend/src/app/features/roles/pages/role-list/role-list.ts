import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import {
  AbstractControl,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { Permission } from '../../models/permission.model';
import { Role } from '../../models/role.model';
import { PermissionService } from '../../services/permission.service';
import { RoleService } from '../../services/role.service';

function requiredArray(control: AbstractControl): ValidationErrors | null {
  const value = control.value as number[];
  return value && value.length > 0 ? null : { required: true };
}

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './role-list.html',
  styleUrl: './role-list.scss',
})
export class RoleList implements OnInit {
  readonly roles = signal<Role[]>([]);
  readonly permissions = signal<Permission[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showModal = signal(false);

  search = '';
  selectedRole: Role | null = null;

  readonly roleForm;

  readonly filteredRoles = computed(() => {
    const keyword = this.search.trim().toLowerCase();

    if (!keyword) {
      return this.roles();
    }

    return this.roles().filter(role =>
      role.name.toLowerCase().includes(keyword) ||
      role.description?.toLowerCase().includes(keyword),
    );
  });

  readonly groupedPermissions = computed(() => {
    const groups = new Map<string, Permission[]>();

    this.permissions().forEach(permission => {
      const module = permission.module || 'OTHER';

      if (!groups.has(module)) {
        groups.set(module, []);
      }

      groups.get(module)?.push(permission);
    });

    return Array.from(groups.entries()).map(([module, permissions]) => ({
      module,
      permissions,
    }));
  });

  constructor(
    private roleService: RoleService,
    private permissionService: PermissionService,
    private alert: AlertService,
    private fb: NonNullableFormBuilder,
  ) {
    this.roleForm = this.fb.group({
      name: this.fb.control('', [Validators.required]),
      description: this.fb.control(''),
      permissionIds: this.fb.control<number[]>([], [requiredArray]),
    });
  }

  ngOnInit(): void {
    this.loadRoles();
    this.loadPermissions();
  }

  loadRoles(): void {
    this.loading.set(true);

    this.roleService.getAll().subscribe({
      next: (res) => {
        this.roles.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadPermissions(): void {
    this.permissionService.getAll().subscribe({
      next: (res) => this.permissions.set(res.data),
    });
  }

  openCreateModal(): void {
    this.selectedRole = null;
    this.roleForm.reset();
    this.roleForm.patchValue({ permissionIds: [] });
    this.showModal.set(true);
  }

  openEditModal(role: Role): void {
    if (this.isSuperAdmin(role)) {
      this.alert.warning('SUPER_ADMIN role cannot be updated');
      return;
    }

    this.selectedRole = role;

    this.roleForm.reset();
    this.roleForm.patchValue({
      name: role.name,
      description: role.description ?? '',
      permissionIds: role.permissions.map(permission => permission.id),
    });

    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.selectedRole = null;
  }

  togglePermission(permissionId: number): void {
    const current = this.roleForm.controls.permissionIds.value;

    const updated = current.includes(permissionId)
      ? current.filter(id => id !== permissionId)
      : [...current, permissionId];

    this.roleForm.controls.permissionIds.setValue(updated);
    this.roleForm.controls.permissionIds.markAsTouched();
  }

  hasPermissionSelected(permissionId: number): boolean {
    return this.roleForm.controls.permissionIds.value.includes(permissionId);
  }

  submitRole(): void {
    if (this.roleForm.invalid) {
      this.roleForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const request = this.roleForm.getRawValue();

    const apiCall = this.selectedRole
      ? this.roleService.update(this.selectedRole.id, request)
      : this.roleService.create(request);

    apiCall.subscribe({
      next: () => {
        this.submitting.set(false);
        this.showModal.set(false);
        this.alert.success(
          this.selectedRole ? 'Role updated successfully' : 'Role created successfully',
        );
        this.selectedRole = null;
        this.loadRoles();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save role');
      },
    });
  }

  isSuperAdmin(role: Role): boolean {
    return role.name.toUpperCase() === 'SUPER_ADMIN';
  }
}
import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Permission } from '../../../roles/models/permission.model';
import { PermissionService } from '../../../roles/services/permission.service';

@Component({
  selector: 'app-permission-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './permission-list.html',
  styleUrl: './permission-list.scss',
})
export class PermissionList implements OnInit {
  readonly permissions = signal<Permission[]>([]);
  readonly loading = signal(false);

  readonly search = signal('');
  readonly selectedModule = signal('');

  readonly modules = computed(() => {
    const uniqueModules = new Set(this.permissions().map(p => p.module));
    return Array.from(uniqueModules).sort();
  });

  readonly filteredPermissions = computed(() => {
    const keyword = this.search().trim().toLowerCase();
    const module = this.selectedModule();

    return this.permissions().filter(permission => {
      const matchesSearch =
        !keyword ||
        permission.name.toLowerCase().includes(keyword) ||
        permission.code.toLowerCase().includes(keyword) ||
        permission.module.toLowerCase().includes(keyword);

      const matchesModule =
        !module || permission.module === module;

      return matchesSearch && matchesModule;
    });
  });

  readonly groupedPermissions = computed(() => {
    const groups = new Map<string, Permission[]>();

    this.filteredPermissions().forEach(permission => {
      const module = permission.module || 'OTHER';

      if (!groups.has(module)) {
        groups.set(module, []);
      }

      groups.get(module)?.push(permission);
    });

    return Array.from(groups.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([module, permissions]) => ({
        module,
        permissions: permissions.sort((a, b) => a.name.localeCompare(b.name)),
      }));
  });

  constructor(private permissionService: PermissionService) {}

  ngOnInit(): void {
    this.loadPermissions();
  }

  loadPermissions(): void {
    this.loading.set(true);

    this.permissionService.getAll().subscribe({
      next: (res) => {
        this.permissions.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  clearFilter(): void {
    this.search.set('');
    this.selectedModule.set('');
  }
}
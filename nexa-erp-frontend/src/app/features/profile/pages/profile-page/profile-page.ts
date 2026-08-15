import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../../core/auth/auth.service';
import { AlertService } from '../../../../core/services/alert.service';
import { resolveFileUrl } from '../../../../shared/utils/file-url.util';
import { ProfileService } from '../../services/profile.service';

const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/png'];
const MAX_PHOTO_SIZE = 2 * 1024 * 1024; // 2 MB — keep in sync with backend limit

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage implements OnDestroy {
  @ViewChild('fileInput') private fileInputRef?: ElementRef<HTMLInputElement>;

  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly alertService = inject(AlertService);

  readonly currentUser = this.authService.currentUser;

  readonly name = signal(this.currentUser()?.name ?? '');
  readonly savingName = signal(false);

  readonly selectedFile = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly uploadingPhoto = signal(false);
  readonly removingPhoto = signal(false);

  readonly displayRole = computed(() => {
    const roles = this.currentUser()?.roles ?? [];
    if (roles.length === 0) return 'Team Member';
    return roles[0]
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  });

  readonly initials = computed(() =>
    (this.currentUser()?.name ?? '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join(''),
  );

  // Preview (if selecting a new photo) takes priority over the saved photo
  readonly avatarUrl = computed(
    () => this.previewUrl() ?? resolveFileUrl(this.currentUser()?.profileImageUrl),
  );

  readonly isPreviewing = computed(() => this.selectedFile() !== null);

  readonly isNameDirty = computed(() => {
    const trimmed = this.name().trim();
    return trimmed.length > 0 && trimmed !== (this.currentUser()?.name ?? '');
  });

  ngOnDestroy(): void {
    this.revokePreview();
  }

  onNameChange(value: string): void {
    this.name.set(value);
  }

  saveName(): void {
    if (!this.isNameDirty() || this.savingName()) return;

    this.savingName.set(true);
    this.profileService.updateName({ name: this.name().trim() }).subscribe({
      next: () => {
        this.savingName.set(false);
        this.alertService.success('Profile updated');
      },
      error: (err) => {
        this.savingName.set(false);
        this.alertService.error(err?.error?.message ?? 'Could not update profile');
      },
    });
  }

  triggerFileSelect(): void {
    this.fileInputRef?.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = ''; // allow re-selecting the same file later

    if (!file) return;

    if (!ALLOWED_PHOTO_TYPES.includes(file.type)) {
      this.alertService.error('Only JPG and PNG images are allowed');
      return;
    }

    if (file.size > MAX_PHOTO_SIZE) {
      this.alertService.error('Profile photo must not exceed 2 MB');
      return;
    }

    this.revokePreview();
    this.selectedFile.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
  }

  cancelPhotoSelection(): void {
    this.revokePreview();
    this.selectedFile.set(null);
  }

  confirmUploadPhoto(): void {
    const file = this.selectedFile();
    if (!file || this.uploadingPhoto()) return;

    this.uploadingPhoto.set(true);
    this.profileService.uploadPhoto(file).subscribe({
      next: () => {
        this.uploadingPhoto.set(false);
        this.revokePreview();
        this.selectedFile.set(null);
        this.alertService.success('Profile photo updated');
      },
      error: (err) => {
        this.uploadingPhoto.set(false);
        this.alertService.error(err?.error?.message ?? 'Could not upload photo');
      },
    });
  }

  async removePhoto(): Promise<void> {
    if (this.removingPhoto()) return;

    const confirmed = await this.alertService.confirm('Remove your profile photo?');
    if (!confirmed) return;

    this.removingPhoto.set(true);
    this.profileService.removePhoto().subscribe({
      next: () => {
        this.removingPhoto.set(false);
        this.alertService.success('Profile photo removed');
      },
      error: (err) => {
        this.removingPhoto.set(false);
        this.alertService.error(err?.error?.message ?? 'Could not remove photo');
      },
    });
  }

  private revokePreview(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
    this.previewUrl.set(null);
  }
}

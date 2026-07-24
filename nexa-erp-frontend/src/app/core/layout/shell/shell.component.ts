import { Component, DestroyRef, HostListener, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { HeaderComponent } from '../header/header.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, SidebarComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly mobileBreakpoint = 992;

  /**
   * Desktop/tablet collapsed sidebar state.
   */
  readonly sidebarCollapsed = signal(false);

  /**
   * Mobile and tablet drawer state.
   */
  readonly mobileSidebarOpen = signal(false);

  /**
   * Current viewport mode.
   */
  readonly isMobileViewport = signal(false);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.updateViewportState(window.innerWidth);
    }

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        /*
         * Mobile sidebar closes automatically
         * after navigation.
         */
        this.closeMobileSidebar();
      });
  }

  toggleSidebar(): void {
    if (this.isMobileViewport()) {
      this.mobileSidebarOpen.update((currentValue) => !currentValue);

      return;
    }

    this.sidebarCollapsed.update((currentValue) => !currentValue);
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }

  @HostListener('window:resize', ['$event'])
  onWindowResize(event: Event): void {
    const target = event.target as Window;

    this.updateViewportState(target.innerWidth);
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    this.closeMobileSidebar();
  }

  private updateViewportState(width: number): void {
    const mobile = width < this.mobileBreakpoint;

    this.isMobileViewport.set(mobile);

    if (mobile) {
     
      this.sidebarCollapsed.set(false);
    } else {
      
      this.mobileSidebarOpen.set(false);
    }
  }
}

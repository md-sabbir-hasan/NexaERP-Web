import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppMenuItem } from '../../models/menu.model';
import { MenuService } from '../../services/menu.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {

  @Input() collapsed = false;

  readonly menu: AppMenuItem[];

  constructor(private menuService: MenuService) {
    this.menu = this.menuService.getMenu();
  }

  trackByLabel(_: number, item: AppMenuItem): string {
    return item.label;
  }
}
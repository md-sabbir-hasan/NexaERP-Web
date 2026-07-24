import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core';
import { TokenService } from '../../core/services/token.service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective {
  private hasView = false;

  constructor(
    private templateRef: TemplateRef<unknown>,
    private viewContainer: ViewContainerRef,
    private tokenService: TokenService,
  ) {}

  @Input() set appHasPermission(permission: string) {
    const allowed = this.tokenService.hasPermission(permission);

    if (allowed && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    }

    if (!allowed && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}

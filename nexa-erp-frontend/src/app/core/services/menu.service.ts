import { Injectable } from '@angular/core';
import { PERMISSIONS } from '../constants/permission.constants';
import { APP_ROUTES } from '../constants/route.constants';
import { AppMenuItem } from '../models/menu.model';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root',
})
export class MenuService {
  constructor(private tokenService: TokenService) {}

  getMenu(): AppMenuItem[] {
    const menu: AppMenuItem[] = [
      {
        label: 'Dashboard',
        icon: 'bi-speedometer2',
        route: APP_ROUTES.DASHBOARD,
      },

      {
        label: 'Administration',
        icon: 'bi-shield-lock',
        children: [
          {
            label: 'Users',
            icon: 'bi-people',
            route: APP_ROUTES.USERS,
            permission: PERMISSIONS.MANAGE_USERS,
          },

          {
            label: 'Roles',
            icon: 'bi-person-badge',
            route: APP_ROUTES.ROLES,
            permission: PERMISSIONS.MANAGE_ROLES,
          },

          {
            label: 'Permissions',
            icon: 'bi-key',
            route: APP_ROUTES.PERMISSIONS,
            permission: PERMISSIONS.MANAGE_PERMISSIONS,
          },

          {
            label: 'Settings',
            icon: 'bi-gear',
            route: APP_ROUTES.SETTINGS,
            permission: PERMISSIONS.MANAGE_SETTINGS,
          },
        ],
      },

      {
        label: 'Finance',
        icon: 'bi-cash-coin',
        children: [
          {
            label: 'Fiscal Years',
            icon: 'bi-calendar-range',
            route: APP_ROUTES.FISCAL_YEAR,
            permission: PERMISSIONS.VIEW_FISCAL_YEAR,
          },
          {
            label: 'Accounting Periods',
            icon: 'bi-calendar3',
            route: APP_ROUTES.ACCOUNTING_PERIOD,
            permission: PERMISSIONS.VIEW_ACCOUNTING_PERIOD,
          },

          {
            label: 'Chart of Accounts',
            icon: 'bi-diagram-3',
            route: APP_ROUTES.ACCOUNTS,
            permission: PERMISSIONS.VIEW_ACCOUNTS,
          },

          {
            label: 'Journal Entry',
            icon: 'bi-journal-text',
            route: APP_ROUTES.JOURNAL,
            permission: PERMISSIONS.VIEW_JOURNAL,
          },

          {
            label: 'Invoices',
            icon: 'bi-receipt',
            route: APP_ROUTES.INVOICE,
            permission: PERMISSIONS.VIEW_INVOICE,
          },

          {
            label: 'Vendor Bills',
            icon: 'bi-file-earmark-text',
            route: APP_ROUTES.VENDOR_BILL,
            permission: PERMISSIONS.VIEW_VENDOR_BILL,
          },

          {
            label: 'Expenses',
            icon: 'bi-wallet2',
            route: APP_ROUTES.EXPENSE,
            permission: PERMISSIONS.VIEW_EXPENSE,
          },

          {
            label: 'Recurring Expenses',
            icon: 'bi-arrow-repeat',
            route: APP_ROUTES.RECURRING_EXPENSE,
            permission: PERMISSIONS.VIEW_RECURRING_EXPENSE,
          },

          {
            label: 'Payments',
            icon: 'bi-credit-card',
            route: APP_ROUTES.PAYMENT,
            permission: PERMISSIONS.VIEW_PAYMENT,
          },

          {
            label: 'Parties',
            icon: 'bi-person-lines-fill',
            route: APP_ROUTES.PARTY,
            permission: PERMISSIONS.VIEW_PARTY,
          },

          {
            label: 'Banking',
            icon: 'bi-bank',
            route: APP_ROUTES.BANKING,
            permission: PERMISSIONS.VIEW_BANKING,
          },

          {
            label: 'Fixed Assets',
            icon: 'bi-building-gear',
            route: APP_ROUTES.FIXED_ASSETS,
            permission: PERMISSIONS.VIEW_FIXED_ASSET
          },
          {
            label: 'Bank Reconciliation',
            icon: 'bi-clipboard2-check',
            route: APP_ROUTES.BANK_RECONCILIATION,
            permission: PERMISSIONS.VIEW_BANKING
          },

          {
            label: 'Budgets',
            icon: 'bi-pie-chart-fill',
            route: APP_ROUTES.BUDGET,
            permission: PERMISSIONS.VIEW_BUDGET,
          },
          {
            label: 'Credit Notes',
            icon: 'bi-receipt-cutoff',
            route: APP_ROUTES.CREDIT_NOTE,
            permission: PERMISSIONS.VIEW_CREDIT_NOTE,
          },

          {
            label: 'Debit Notes',
            icon: 'bi-receipt-cutoff',
            route: APP_ROUTES.DEBIT_NOTE,
            permission: PERMISSIONS.VIEW_DEBIT_NOTE,
          },
        ],
      },

      {
        label: 'Reports',
        icon: 'bi-bar-chart',
        children: [
          {
            label: 'Ledger',
            icon: 'bi-journal',
            route: APP_ROUTES.LEDGER,
            permission: PERMISSIONS.VIEW_LEDGER,
          },

          {
            label: 'Trial Balance',
            icon: 'bi-table',
            route: APP_ROUTES.TRIAL_BALANCE,
            permission: PERMISSIONS.VIEW_TRIAL_BALANCE,
          },

          {
            label: 'Reports',
            icon: 'bi-graph-up',
            route: APP_ROUTES.REPORTS,
            permission: PERMISSIONS.VIEW_REPORT,
          },
        ],
      },

      {
        label: 'Audit Logs',
        icon: 'bi-clock-history',
        route: APP_ROUTES.AUDIT,
        permission: PERMISSIONS.VIEW_AUDIT_LOGS,
      },
    ];

    return this.filterByPermission(menu);
  }

  private filterByPermission(menu: AppMenuItem[]): AppMenuItem[] {
    return menu
      .map((item) => ({
        ...item,
        children: item.children ? this.filterByPermission(item.children) : undefined,
      }))
      .filter((item) => {
        const hasPermission = !item.permission || this.tokenService.hasPermission(item.permission);

        const hasVisibleChildren = !item.children || item.children.length > 0;

        return hasPermission && hasVisibleChildren;
      });
  }
}

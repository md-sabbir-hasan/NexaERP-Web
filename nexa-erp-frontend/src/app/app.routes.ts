// import { Routes } from '@angular/router';
// import { authGuard } from './core/guards/auth.guard';
// import { guestGuard } from './core/guards/guest.guard';
// import { ShellComponent } from './core/layout/shell/shell.component';
// import { LoginComponent } from './features/auth/pages/login/login.component';
// import { DashboardComponent } from './features/dashboard/pages/dashboard/dashboard.component';
// import { permissionGuard } from './core/guards/permission.guard';
// import { PERMISSIONS } from './core/constants/permission.constants';

// export const routes: Routes = [
//   {
//     path: 'login',
//     component: LoginComponent,
//     canActivate: [guestGuard],
//   },
//   {
//     path: 'forgot-password',
//     loadComponent: () =>
//       import('./features/auth/pages/forgot-password/forgot-password').then((m) => m.ForgotPassword),
//     canActivate: [guestGuard],
//   },
//   {
//     path: 'reset-password',
//     loadComponent: () =>
//       import('./features/auth/pages/reset-password/reset-password').then((m) => m.ResetPassword),
//     canActivate: [guestGuard],
//   },
//   {
//     path: 'set-password',
//     loadComponent: () =>
//       import('./features/auth/pages/set-password/set-password').then((m) => m.SetPassword),
//   },
//   {
//     path: '',
//     component: ShellComponent,
//     canActivate: [authGuard],
//     children: [
//       {
//         path: 'access-denied',
//         loadComponent: () =>
//           import('./shared/pages/access-denied/access-denied')
//             .then((m) => m.AccessDenied),
//       },
//       {
//         path: 'dashboard',
//         component: DashboardComponent,
//       },

//       {
//         path: 'users',
//         loadComponent: () =>
//           import('./features/users/pages/user-list/user-list').then((m) => m.UserList),
//       },
//       {
//         path: 'roles',
//         loadComponent: () =>
//           import('./features/roles/pages/role-list/role-list').then((m) => m.RoleList),
//       },
//       {
//         path: 'permissions',
//         loadComponent: () =>
//           import('./features/permissions/pages/permission-list/permission-list').then(
//             (m) => m.PermissionList,
//           ),
//       },

//       {
//         path: 'audit',
//         loadComponent: () =>
//           import('./features/audit/pages/audit-log-list/audit-log-list').then(
//             (m) => m.AuditLogList,
//           ),
//       },

//       {
//         path: 'fiscal-years',
//         loadComponent: () =>
//           import('./features/fiscal-year/pages/fiscal-year-list/fiscal-year-list').then(
//             (m) => m.FiscalYearList,
//           ),
//       },
//       {
//         path: 'accounting-periods',
//         loadComponent: () =>
//           import('./features/accounting-period/pages/accounting-period-list/accounting-period-list').then(
//             (m) => m.AccountingPeriodList,
//           ),
//       },

//       {
//         path: 'accounts',
//         loadComponent: () =>
//           import('./features/accounts/pages/account-list/account-list').then((m) => m.AccountList),
//       },

//       {
//         path: 'journals',
//         loadComponent: () =>
//           import('./features/journal/pages/journal-list/journal-list')
//             .then((m) => m.JournalList),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_JOURNAL,
//         },
//       },
//       {
//         path: 'journals/new',
//         loadComponent: () =>
//           import('./features/journal/pages/journal-form/journal-form')
//             .then((m) => m.JournalForm),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.CREATE_JOURNAL,
//         },
//       },
//       {
//         path: 'journals/:id/edit',
//         loadComponent: () =>
//           import('./features/journal/pages/journal-form/journal-form')
//             .then((m) => m.JournalForm),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.CREATE_JOURNAL,
//         },
//       },
//       {
//         path: 'reports/ledger',
//         loadComponent: () => import('./features/reports/pages/ledger/ledger').then((m) => m.Ledger),
//       },
//       {
//         path: 'reports/trial-balance',
//         loadComponent: () =>
//           import('./features/reports/pages/trial-balance/trial-balance').then(
//             (m) => m.TrialBalance,
//           ),
//       },
//       {
//         path: 'invoice',
//         loadComponent: () =>
//           import('./features/invoice/pages/invoice-list/invoice-list')
//             .then((m) => m.InvoiceList),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_INVOICE,
//         },
//       },
//       {
//         path: 'invoice/new',
//         loadComponent: () =>
//           import('./features/invoice/pages/invoice-form/invoice-form')
//             .then((m) => m.InvoiceForm),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.CREATE_INVOICE,
//         },
//       },
//       {
//         path: 'invoice/:id/edit',
//         loadComponent: () =>
//           import('./features/invoice/pages/invoice-form/invoice-form')
//             .then((m) => m.InvoiceForm),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.EDIT_INVOICE,
//         },
//       },
//       {
//         path: 'invoice/:id',
//         loadComponent: () =>
//           import('./features/invoice/pages/invoice-details/invoice-details')
//             .then((m) => m.InvoiceDetails),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_INVOICE,
//         },
//       },

//       {
//         path: 'vendor-bill',
//         loadComponent: () =>
//           import('./features/vendor-bill/pages/vendor-bill-list/vendor-bill-list').then(
//             (m) => m.VendorBillList,
//           ),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_VENDOR_BILL,
//         },
//       },
//       {
//         path: 'vendor-bill/new',
//         loadComponent: () =>
//           import('./features/vendor-bill/pages/vendor-bill-form/vendor-bill-form').then(
//             (m) => m.VendorBillForm,
//           ),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.CREATE_VENDOR_BILL,
//         },
//       },
//       {
//         path: 'vendor-bill/:id/edit',
//         loadComponent: () =>
//           import('./features/vendor-bill/pages/vendor-bill-form/vendor-bill-form').then(
//             (m) => m.VendorBillForm,
//           ),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.EDIT_VENDOR_BILL,
//         },
//       },
//       {
//         path: 'vendor-bill/:id',
//         loadComponent: () =>
//           import('./features/vendor-bill/pages/vendor-bill-details/vendor-bill-details').then(
//             (m) => m.VendorBillDetails,
//           ),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_VENDOR_BILL,
//         },
//       },

//       {
//         path: 'expense',
//         loadComponent: () =>
//           import('./features/expense/pages/expense-list/expense-list')
//             .then((m) => m.ExpenseList),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_EXPENSE,
//         },
//       },
//       {
//         path: 'expense/new',
//         loadComponent: () =>
//           import('./features/expense/pages/expense-form/expense-form')
//             .then((m) => m.ExpenseForm),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.CREATE_EXPENSE,
//         },
//       },
//       {
//         path: 'expense/:id',
//         loadComponent: () =>
//           import('./features/expense/pages/expense-detail/expense-detail')
//             .then((m) => m.ExpenseDetail),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_EXPENSE,
//         },
//       },

//       // Recurring Expense
//       {
//         path: 'recurring-expense',
//         loadComponent: () =>
//           import('./features/recurring-expense/pages/recurring-expense-list/recurring-expense-list')
//             .then((m) => m.RecurringExpenseList),
//       },
//       {
//         path: 'recurring-expense/new',
//         loadComponent: () =>
//           import('./features/recurring-expense/pages/recurring-expense-form/recurring-expense-form')
//             .then((m) => m.RecurringExpenseForm),
//       },

//       // Budget
//       {
//         path: 'budget',
//         loadComponent: () =>
//           import('./features/budget/pages/budget-list/budget-list').then((m) => m.BudgetList),
//       },
//       {
//         path: 'budget/new',
//         loadComponent: () =>
//           import('./features/budget/pages/budget-form/budget-form').then((m) => m.BudgetForm),
//       },
//       {
//         path: 'budget/:id',
//         loadComponent: () =>
//           import('./features/budget/pages/budget-detail/budget-detail').then((m) => m.BudgetDetail),
//       },
//       {
//         path: 'budget/:id/variance',
//         loadComponent: () =>
//           import('./features/budget/pages/budget-variance/budget-variance').then((m) => m.BudgetVariance),
//       },

//       // Payment
//       {
//         path: 'payment',
//         loadComponent: () =>
//           import('./features/payment/pages/payment-list/payment-list')
//             .then((m) => m.PaymentList),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_PAYMENT,
//         },
//       },
//       {
//         path: 'payment/new',
//         loadComponent: () =>
//           import('./features/payment/pages/payment-form/payment-form')
//             .then((m) => m.PaymentForm),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.CREATE_PAYMENT,
//         },
//       },
//       {
//         path: 'payment/:id',
//         loadComponent: () =>
//           import('./features/payment/pages/payment-details/payment-details')
//             .then((m) => m.PaymentDetails),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_PAYMENT,
//         },
//       },
//       //banking
//       {
//         path: 'banking',
//         loadComponent: () =>
//           import('./features/banking/pages/bank-account-list/bank-account-list').then(
//             (m) => m.BankAccountList),
//         canActivate: [permissionGuard],
//         data: {
//           permission: PERMISSIONS.VIEW_BANKING,
//         },
//       },
//       {
//         path: 'banking/transactions',
//         loadComponent: () =>
//           import('./features/banking/pages/bank-transaction-list/bank-transaction-list').then(
//             (m) => m.BankTransactionList,
//           ),
//           data: {
//           permission: PERMISSIONS.VIEW_BANKING,
//         },
//       },
//       {
//         path: 'banking/reconciliation',
//         loadComponent: () =>
//           import('./features/banking/pages/bank-reconciliation-list/bank-reconciliation-list').then(
//             (m) => m.BankReconciliationList,
//           ),
//           data: {
//           permission: PERMISSIONS.VIEW_BANKING,
//         },
//       },
//       {
//         path: 'banking/reconciliation/:id',
//         loadComponent: () =>
//           import('./features/banking/pages/bank-reconciliation-detail/bank-reconciliation-detail').then(
//             (m) => m.BankReconciliationDetail,
//           ),
//           data: {
//           permission: PERMISSIONS.EDIT_BANKING,
//         },
//       },
//       {
//         path: 'banking/reconciliation',
//         loadComponent: () =>
//           import('./features/banking/pages/bank-reconciliation-list/bank-reconciliation-list').then(
//             (m) => m.BankReconciliationList,
//           ),
//           data: {
//           permission: PERMISSIONS.VIEW_BANKING,
//         },
//       },
//       {
//         path: 'banking/reconciliation/:id',
//         loadComponent: () =>
//           import('./features/banking/pages/bank-reconciliation-detail/bank-reconciliation-detail').then(
//             (m) => m.BankReconciliationDetail,
//           ),
//           data: {
//           permission: PERMISSIONS.EDIT_BANKING,
//         },
//       },

//       {
//         path: 'fixed-assets',
//         loadComponent: () =>
//           import('./features/fixed-assets/pages/fixed-asset-list/fixed-asset-list').then(
//             (m) => m.FixedAssetList,
//           ),
//       },
//       {
//         path: 'fixed-assets/:id',
//         loadComponent: () =>
//           import('./features/fixed-assets/pages/fixed-asset-detail/fixed-asset-detail').then(
//             (m) => m.FixedAssetDetail,
//           ),
//       },

//       {
//         path: 'party',
//         loadComponent: () =>
//           import('./features/party/pages/party-list/party-list').then((m) => m.PartyList),
//       },
//       {
//         path: 'party/new',
//         loadComponent: () =>
//           import('./features/party/pages/party-form/party-form').then((m) => m.PartyForm),
//       },
//       {
//         path: 'party/:id',
//         loadComponent: () =>
//           import('./features/party/pages/party-details/party-details').then((m) => m.PartyDetails),
//       },
//       {
//         path: 'party/:id/edit',
//         loadComponent: () =>
//           import('./features/party/pages/party-form/party-form').then((m) => m.PartyForm),
//       },
//       {
//         path: 'reports',
//         loadComponent: () =>
//           import('./features/reports/pages/reports-dashboard/reports-dashboard').then(
//             (m) => m.ReportsDashboard,
//           ),
//       },

//       {
//         path: 'reports/party-statement',
//         loadComponent: () =>
//           import('./features/reports/pages/party-statement-report/party-statement-report').then(
//             (m) => m.PartyStatementReport,
//           ),
//       },
//       {
//         path: 'reports/profit-loss',
//         loadComponent: () =>
//           import('./features/reports/pages/profit-loss-report/profit-loss-report').then(
//             (m) => m.ProfitLossReport,
//           ),
//       },
//       {
//         path: 'reports/balance-sheet',
//         loadComponent: () =>
//           import('./features/reports/pages/balance-sheet-report/balance-sheet-report').then(
//             (m) => m.BalanceSheetReport,
//           ),
//       },
//       {
//         path: 'reports/aging',
//         loadComponent: () =>
//           import('./features/reports/pages/aging-report/aging-report').then((m) => m.AgingReport),
//       },

//       {
//         path: 'settings',
//         loadComponent: () =>
//           import('./features/settings/pages/settings-list/settings-list').then(
//             (m) => m.SettingsList,
//           ),
//       },

//       {
//         path: 'credit-notes',
//         loadComponent: () =>
//           import('./features/credit-note/pages/credit-note-list/credit-note-list').then(
//             (m) => m.CreditNoteList,
//           ),
//       },
//       {
//         path: 'credit-notes/new',
//         loadComponent: () =>
//           import('./features/credit-note/pages/credit-note-form/credit-note-form').then(
//             (m) => m.CreditNoteForm,
//           ),
//       },
//       {
//         path: 'credit-notes/:id/edit',
//         loadComponent: () =>
//           import('./features/credit-note/pages/credit-note-form/credit-note-form').then(
//             (m) => m.CreditNoteForm,
//           ),
//       },

//       {
//         path: 'debit-notes',
//         loadComponent: () =>
//           import('./features/debit-note/pages/debit-note-list/debit-note-list').then(
//             (m) => m.DebitNoteList,
//           ),
//       },
//       {
//         path: 'debit-notes/new',
//         loadComponent: () =>
//           import('./features/debit-note/pages/debit-note-form/debit-note-form').then(
//             (m) => m.DebitNoteForm,
//           ),
//       },
//       {
//         path: 'debit-notes/:id/edit',
//         loadComponent: () =>
//           import('./features/debit-note/pages/debit-note-form/debit-note-form').then(
//             (m) => m.DebitNoteForm,
//           ),
//       },

//       {
//         path: '',
//         pathMatch: 'full',
//         redirectTo: 'dashboard',
//       },
//     ],
//   },
//   {
//     path: '**',
//     redirectTo: 'dashboard',
//   },
// ];




import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { permissionGuard } from './core/guards/permission.guard';

import { PERMISSIONS } from './core/constants/permission.constants';

import { ShellComponent } from './core/layout/shell/shell.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { DashboardComponent } from './features/dashboard/pages/dashboard/dashboard.component';

export const routes: Routes = [
  // =========================================================
  // Public / Authentication
  // =========================================================

  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import(
        './features/auth/pages/forgot-password/forgot-password'
      ).then((m) => m.ForgotPassword),
    canActivate: [guestGuard],
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import(
        './features/auth/pages/reset-password/reset-password'
      ).then((m) => m.ResetPassword),
    canActivate: [guestGuard],
  },
  {
    path: 'set-password',
    loadComponent: () =>
      import(
        './features/auth/pages/set-password/set-password'
      ).then((m) => m.SetPassword),
  },

  // =========================================================
  // Authenticated Application
  // =========================================================

  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],

    children: [
      // =====================================================
      // Common
      // =====================================================

      {
        path: 'access-denied',
        loadComponent: () =>
          import(
            './shared/pages/access-denied/access-denied'
          ).then((m) => m.AccessDenied),
      },
      {
        path: 'dashboard',
        component: DashboardComponent,
      },

      // =====================================================
      // Administration
      // =====================================================

      {
        path: 'users',
        loadComponent: () =>
          import(
            './features/users/pages/user-list/user-list'
          ).then((m) => m.UserList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.MANAGE_USERS,
        },
      },
      {
        path: 'roles',
        loadComponent: () =>
          import(
            './features/roles/pages/role-list/role-list'
          ).then((m) => m.RoleList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.MANAGE_ROLES,
        },
      },
      {
        path: 'permissions',
        loadComponent: () =>
          import(
            './features/permissions/pages/permission-list/permission-list'
          ).then((m) => m.PermissionList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.MANAGE_PERMISSIONS,
        },
      },
      {
        path: 'audit',
        loadComponent: () =>
          import(
            './features/audit/pages/audit-log-list/audit-log-list'
          ).then((m) => m.AuditLogList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_AUDIT_LOGS,
        },
      },

      // =====================================================
      // Fiscal Year
      // =====================================================

      {
        path: 'fiscal-years',
        loadComponent: () =>
          import(
            './features/fiscal-year/pages/fiscal-year-list/fiscal-year-list'
          ).then((m) => m.FiscalYearList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_FISCAL_YEAR,
        },
      },

      // =====================================================
      // Accounting Period
      // =====================================================

      {
        path: 'accounting-periods',
        loadComponent: () =>
          import(
            './features/accounting-period/pages/accounting-period-list/accounting-period-list'
          ).then((m) => m.AccountingPeriodList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_ACCOUNTING_PERIOD,
        },
      },

      // =====================================================
      // Chart of Accounts
      // =====================================================

      {
        path: 'accounts',
        loadComponent: () =>
          import(
            './features/accounts/pages/account-list/account-list'
          ).then((m) => m.AccountList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_ACCOUNTS,
        },
      },

      // =====================================================
      // Journal Entry
      // =====================================================

      {
        path: 'journals',
        loadComponent: () =>
          import(
            './features/journal/pages/journal-list/journal-list'
          ).then((m) => m.JournalList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_JOURNAL,
        },
      },
      {
        path: 'journals/new',
        loadComponent: () =>
          import(
            './features/journal/pages/journal-form/journal-form'
          ).then((m) => m.JournalForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_JOURNAL,
        },
      },
      {
        path: 'journals/:id/edit',
        loadComponent: () =>
          import(
            './features/journal/pages/journal-form/journal-form'
          ).then((m) => m.JournalForm),
        canActivate: [permissionGuard],
        data: {
          // Backend update endpoint currently uses CREATE_JOURNAL.
          permission: PERMISSIONS.CREATE_JOURNAL,
        },
      },

      // =====================================================
      // Invoice
      // =====================================================

      {
        path: 'invoice',
        loadComponent: () =>
          import(
            './features/invoice/pages/invoice-list/invoice-list'
          ).then((m) => m.InvoiceList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_INVOICE,
        },
      },
      {
        path: 'invoice/new',
        loadComponent: () =>
          import(
            './features/invoice/pages/invoice-form/invoice-form'
          ).then((m) => m.InvoiceForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_INVOICE,
        },
      },
      {
        path: 'invoice/:id/edit',
        loadComponent: () =>
          import(
            './features/invoice/pages/invoice-form/invoice-form'
          ).then((m) => m.InvoiceForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.EDIT_INVOICE,
        },
      },
      {
        path: 'invoice/:id',
        loadComponent: () =>
          import(
            './features/invoice/pages/invoice-details/invoice-details'
          ).then((m) => m.InvoiceDetails),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_INVOICE,
        },
      },

      // =====================================================
      // Vendor Bill
      // =====================================================

      {
        path: 'vendor-bill',
        loadComponent: () =>
          import(
            './features/vendor-bill/pages/vendor-bill-list/vendor-bill-list'
          ).then((m) => m.VendorBillList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_VENDOR_BILL,
        },
      },
      {
        path: 'vendor-bill/new',
        loadComponent: () =>
          import(
            './features/vendor-bill/pages/vendor-bill-form/vendor-bill-form'
          ).then((m) => m.VendorBillForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_VENDOR_BILL,
        },
      },
      {
        path: 'vendor-bill/:id/edit',
        loadComponent: () =>
          import(
            './features/vendor-bill/pages/vendor-bill-form/vendor-bill-form'
          ).then((m) => m.VendorBillForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.EDIT_VENDOR_BILL,
        },
      },
      {
        path: 'vendor-bill/:id',
        loadComponent: () =>
          import(
            './features/vendor-bill/pages/vendor-bill-details/vendor-bill-details'
          ).then((m) => m.VendorBillDetails),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_VENDOR_BILL,
        },
      },

      // =====================================================
      // Expense
      // =====================================================

      {
        path: 'expense',
        loadComponent: () =>
          import(
            './features/expense/pages/expense-list/expense-list'
          ).then((m) => m.ExpenseList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_EXPENSE,
        },
      },
      {
        path: 'expense/new',
        loadComponent: () =>
          import(
            './features/expense/pages/expense-form/expense-form'
          ).then((m) => m.ExpenseForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_EXPENSE,
        },
      },
      {
        path: 'expense/:id',
        loadComponent: () =>
          import(
            './features/expense/pages/expense-detail/expense-detail'
          ).then((m) => m.ExpenseDetail),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_EXPENSE,
        },
      },

      // =====================================================
      // Recurring Expense
      // =====================================================

      {
        path: 'recurring-expense',
        loadComponent: () =>
          import(
            './features/recurring-expense/pages/recurring-expense-list/recurring-expense-list'
          ).then((m) => m.RecurringExpenseList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_RECURRING_EXPENSE,
        },
      },
      {
        path: 'recurring-expense/new',
        loadComponent: () =>
          import(
            './features/recurring-expense/pages/recurring-expense-form/recurring-expense-form'
          ).then((m) => m.RecurringExpenseForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_RECURRING_EXPENSE,
        },
      },

      // =====================================================
      // Payment
      // =====================================================

      {
        path: 'payment',
        loadComponent: () =>
          import(
            './features/payment/pages/payment-list/payment-list'
          ).then((m) => m.PaymentList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_PAYMENT,
        },
      },
      {
        path: 'payment/new',
        loadComponent: () =>
          import(
            './features/payment/pages/payment-form/payment-form'
          ).then((m) => m.PaymentForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_PAYMENT,
        },
      },
      {
        path: 'payment/:id',
        loadComponent: () =>
          import(
            './features/payment/pages/payment-details/payment-details'
          ).then((m) => m.PaymentDetails),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_PAYMENT,
        },
      },

      // =====================================================
      // Banking
      // =====================================================

      {
        path: 'banking/transactions',
        loadComponent: () =>
          import(
            './features/banking/pages/bank-transaction-list/bank-transaction-list'
          ).then((m) => m.BankTransactionList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BANKING,
        },
      },
      {
        path: 'banking/reconciliation',
        loadComponent: () =>
          import(
            './features/banking/pages/bank-reconciliation-list/bank-reconciliation-list'
          ).then((m) => m.BankReconciliationList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BANKING,
        },
      },
      {
        path: 'banking/reconciliation/:id',
        loadComponent: () =>
          import(
            './features/banking/pages/bank-reconciliation-detail/bank-reconciliation-detail'
          ).then((m) => m.BankReconciliationDetail),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BANKING,
        },
      },
      {
        path: 'banking',
        loadComponent: () =>
          import(
            './features/banking/pages/bank-account-list/bank-account-list'
          ).then((m) => m.BankAccountList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BANKING,
        },
      },

      // =====================================================
      // Budget
      // =====================================================

      {
        path: 'budget',
        loadComponent: () =>
          import(
            './features/budget/pages/budget-list/budget-list'
          ).then((m) => m.BudgetList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BUDGET,
        },
      },
      {
        path: 'budget/new',
        loadComponent: () =>
          import(
            './features/budget/pages/budget-form/budget-form'
          ).then((m) => m.BudgetForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_BUDGET,
        },
      },
      {
        path: 'budget/:id/variance',
        loadComponent: () =>
          import(
            './features/budget/pages/budget-variance/budget-variance'
          ).then((m) => m.BudgetVariance),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BUDGET_REPORT,
        },
      },
      {
        path: 'budget/:id',
        loadComponent: () =>
          import(
            './features/budget/pages/budget-detail/budget-detail'
          ).then((m) => m.BudgetDetail),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_BUDGET,
        },
      },

      // =====================================================
      // Fixed Assets
      // =====================================================

      {
        path: 'fixed-assets',
        loadComponent: () =>
          import(
            './features/fixed-assets/pages/fixed-asset-list/fixed-asset-list'
          ).then((m) => m.FixedAssetList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_FIXED_ASSET,
        },
      },
      {
        path: 'fixed-assets/:id',
        loadComponent: () =>
          import(
            './features/fixed-assets/pages/fixed-asset-detail/fixed-asset-detail'
          ).then((m) => m.FixedAssetDetail),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_FIXED_ASSET,
        },
      },

      // =====================================================
      // Party
      // =====================================================

      {
        path: 'party',
        loadComponent: () =>
          import(
            './features/party/pages/party-list/party-list'
          ).then((m) => m.PartyList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_PARTY,
        },
      },
      {
        path: 'party/new',
        loadComponent: () =>
          import(
            './features/party/pages/party-form/party-form'
          ).then((m) => m.PartyForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_PARTY,
        },
      },
      {
        path: 'party/:id/edit',
        loadComponent: () =>
          import(
            './features/party/pages/party-form/party-form'
          ).then((m) => m.PartyForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.EDIT_PARTY,
        },
      },
      {
        path: 'party/:id',
        loadComponent: () =>
          import(
            './features/party/pages/party-details/party-details'
          ).then((m) => m.PartyDetails),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_PARTY,
        },
      },

      // =====================================================
      // Reports
      // =====================================================

      {
        path: 'reports',
        loadComponent: () =>
          import(
            './features/reports/pages/reports-dashboard/reports-dashboard'
          ).then((m) => m.ReportsDashboard),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_REPORT,
        },
      },
      {
        path: 'reports/ledger',
        loadComponent: () =>
          import(
            './features/reports/pages/ledger/ledger'
          ).then((m) => m.Ledger),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_LEDGER,
        },
      },
      {
        path: 'reports/trial-balance',
        loadComponent: () =>
          import(
            './features/reports/pages/trial-balance/trial-balance'
          ).then((m) => m.TrialBalance),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_TRIAL_BALANCE,
        },
      },
      {
        path: 'reports/party-statement',
        loadComponent: () =>
          import(
            './features/reports/pages/party-statement-report/party-statement-report'
          ).then((m) => m.PartyStatementReport),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_REPORT,
        },
      },
      {
        path: 'reports/profit-loss',
        loadComponent: () =>
          import(
            './features/reports/pages/profit-loss-report/profit-loss-report'
          ).then((m) => m.ProfitLossReport),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_REPORT,
        },
      },
      {
        path: 'reports/balance-sheet',
        loadComponent: () =>
          import(
            './features/reports/pages/balance-sheet-report/balance-sheet-report'
          ).then((m) => m.BalanceSheetReport),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_REPORT,
        },
      },
      {
        path: 'reports/aging',
        loadComponent: () =>
          import(
            './features/reports/pages/aging-report/aging-report'
          ).then((m) => m.AgingReport),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_REPORT,
        },
      },

      // =====================================================
      // Settings
      // =====================================================

      {
        path: 'settings',
        loadComponent: () =>
          import(
            './features/settings/pages/settings-list/settings-list'
          ).then((m) => m.SettingsList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.MANAGE_SETTINGS,
        },
      },

      // =====================================================
      // Credit Notes
      // =====================================================

      {
        path: 'credit-notes',
        loadComponent: () =>
          import(
            './features/credit-note/pages/credit-note-list/credit-note-list'
          ).then((m) => m.CreditNoteList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_CREDIT_NOTE,
        },
      },
      {
        path: 'credit-notes/new',
        loadComponent: () =>
          import(
            './features/credit-note/pages/credit-note-form/credit-note-form'
          ).then((m) => m.CreditNoteForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_CREDIT_NOTE,
        },
      },
      {
        path: 'credit-notes/:id/edit',
        loadComponent: () =>
          import(
            './features/credit-note/pages/credit-note-form/credit-note-form'
          ).then((m) => m.CreditNoteForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.EDIT_CREDIT_NOTE,
        },
      },

      // =====================================================
      // Debit Notes
      // =====================================================

      {
        path: 'debit-notes',
        loadComponent: () =>
          import(
            './features/debit-note/pages/debit-note-list/debit-note-list'
          ).then((m) => m.DebitNoteList),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.VIEW_DEBIT_NOTE,
        },
      },
      {
        path: 'debit-notes/new',
        loadComponent: () =>
          import(
            './features/debit-note/pages/debit-note-form/debit-note-form'
          ).then((m) => m.DebitNoteForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.CREATE_DEBIT_NOTE,
        },
      },
      {
        path: 'debit-notes/:id/edit',
        loadComponent: () =>
          import(
            './features/debit-note/pages/debit-note-form/debit-note-form'
          ).then((m) => m.DebitNoteForm),
        canActivate: [permissionGuard],
        data: {
          permission: PERMISSIONS.EDIT_DEBIT_NOTE,
        },
      },

      // =====================================================
      // Default
      // =====================================================

      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
    ],
  },

  // =========================================================
  // Global Fallback
  // =========================================================

  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
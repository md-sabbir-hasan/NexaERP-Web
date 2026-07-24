export const APP_ROUTES = {
  LOGIN: '/login',
  DASHBOARD: '/dashboard',

  USERS: '/users',
  ROLES: '/roles',
  PERMISSIONS: '/permissions',
  SETTINGS: '/settings',

  ACCOUNTS: '/accounts',
  JOURNAL: '/journals',
  INVOICE: '/invoice',
  VENDOR_BILL: '/vendor-bill',
  EXPENSE: '/expense',
  PAYMENT: '/payment',
  PARTY: '/party',
  BANKING: '/banking',
  BANK_RECONCILIATION: '/banking/reconciliation',
  FIXED_ASSETS: '/fixed-assets',
  BUDGET: '/budget',
  RECURRING_EXPENSE: '/recurring-expense',

  REPORTS: '/reports',
  LEDGER: '/reports/ledger',
  TRIAL_BALANCE: '/reports/trial-balance',

  AUDIT: '/audit',

  FISCAL_YEAR: '/fiscal-years',
  ACCOUNTING_PERIOD: '/accounting-periods',
  CREDIT_NOTE: '/credit-notes',
  DEBIT_NOTE: '/debit-notes'
} as const;
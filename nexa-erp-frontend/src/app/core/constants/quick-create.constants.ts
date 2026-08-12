import {
  PERMISSIONS,
  PermissionCode,
} from './permission.constants';

export interface QuickCreateItem {
  label: string;
  icon: string;
  route: string;
  permission: PermissionCode;
}

export const QUICK_CREATE_ITEMS: QuickCreateItem[] = [
  {
    label: 'New Invoice',
    icon: 'bi-receipt',
    route: '/invoice/new',
    permission: PERMISSIONS.CREATE_INVOICE,
  },
  {
    label: 'New Vendor Bill',
    icon: 'bi-file-earmark-text',
    route: '/vendor-bill/new',
    permission: PERMISSIONS.CREATE_VENDOR_BILL,
  },
  {
    label: 'New Expense',
    icon: 'bi-wallet2',
    route: '/expense/new',
    permission: PERMISSIONS.CREATE_EXPENSE,
  },
  {
    label: 'New Payment',
    icon: 'bi-cash-stack',
    route: '/payment/new',
    permission: PERMISSIONS.CREATE_PAYMENT,
  },
  {
    label: 'New Journal',
    icon: 'bi-journal-plus',
    route: '/journals/new',
    permission: PERMISSIONS.CREATE_JOURNAL,
  },
  {
    label: 'New Party',
    icon: 'bi-person-plus',
    route: '/party/new',
    permission: PERMISSIONS.CREATE_PARTY,
  },
  {
    label: 'New Recurring Expense',
    icon: 'bi-arrow-repeat',
    route: '/recurring-expense/new',
    permission: PERMISSIONS.CREATE_RECURRING_EXPENSE,
  },
  {
    label: 'New Budget',
    icon: 'bi-bar-chart-line',
    route: '/budget/new',
    permission: PERMISSIONS.CREATE_BUDGET,
  },
  {
    label: 'New Credit Note',
    icon: 'bi-file-earmark-minus',
    route: '/credit-notes/new',
    permission: PERMISSIONS.CREATE_CREDIT_NOTE,
  },
  {
    label: 'New Debit Note',
    icon: 'bi-file-earmark-plus',
    route: '/debit-notes/new',
    permission: PERMISSIONS.CREATE_DEBIT_NOTE,
  },
];
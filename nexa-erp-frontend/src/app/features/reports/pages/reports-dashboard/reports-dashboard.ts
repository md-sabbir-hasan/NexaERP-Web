import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TokenService } from '../../../../core/services/token.service';
import { PERMISSIONS } from '../../../../core/constants/permission.constants';

interface ReportCard {
  title: string;
  description: string;
  icon: string;
  route: string;
  status: 'READY' | 'COMING_SOON';
  permission?: string;
}

@Component({
  selector: 'app-reports-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './reports-dashboard.html',
  styleUrl: './reports-dashboard.scss',
})
export class ReportsDashboard {
  private readonly allReports: ReportCard[] = [
    {
      title: 'Ledger',
      description: 'Account-wise debit, credit and running balance.',
      icon: 'bi bi-journal-text',
      route: '/reports/ledger',
      status: 'READY',
    },
    {
      title: 'Trial Balance',
      description: 'Debit and credit balance summary for all accounts.',
      icon: 'bi bi-list-check',
      route: '/reports/trial-balance',
      status: 'READY',
    },
    {
      title: 'Profit & Loss',
      description: 'Revenue, expenses and net profit/loss statement.',
      icon: 'bi bi-graph-up-arrow',
      route: '/reports/profit-loss',
      status: 'READY',
    },
    {
      title: 'Balance Sheet',
      description: 'Assets, liabilities and equity as of a selected date.',
      icon: 'bi bi-bank',
      route: '/reports/balance-sheet',
      status: 'READY',
    },
    {
      title: 'Cash Flow Statement',
      description: 'Direct-method operating, investing and financing cash movements.',
      icon: 'bi bi-cash-stack',
      route: '/reports/cash-flow',
      status: 'READY',
    },
    {
      title: 'Budget vs Actual',
      description: 'Compare period budget allocations with posted Revenue and Expense activity.',
      icon: 'bi bi-bar-chart-line',
      route: '/reports/budget-vs-actual',
      status: 'READY',
      permission: PERMISSIONS.VIEW_BUDGET_REPORT,
    },
    {
      title: 'Party Statement',
      description: 'Customer/vendor transaction statement.',
      icon: 'bi bi-people',
      route: '/reports/party-statement',
      status: 'READY',
    },
    {
      title: 'Aging Report',
      description: 'Customer receivable and vendor payable aging.',
      icon: 'bi bi-calendar-range',
      route: '/reports/aging',
      status: 'READY',
    },
  ];

  readonly reports: ReportCard[];

  constructor(tokenService: TokenService) {
    this.reports = this.allReports.filter(
      (report) => !report.permission || tokenService.hasPermission(report.permission),
    );
  }
}

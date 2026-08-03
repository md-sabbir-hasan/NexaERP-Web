import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApprovalService } from './approval.service';
import { VendorBillService } from '../../vendor-bill/services/vendor-bill.service';
import { InvoiceService } from '../../invoice/services/invoice.service';

describe('Document approval APIs', () => {
  let http: HttpTestingController;
  let approvals: ApprovalService;
  let vendorBills: VendorBillService;
  let invoices: InvoiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ApprovalService, VendorBillService, InvoiceService],
    });
    http = TestBed.inject(HttpTestingController);
    approvals = TestBed.inject(ApprovalService);
    vendorBills = TestBed.inject(VendorBillService);
    invoices = TestBed.inject(InvoiceService);
  });

  afterEach(() => http.verify());

  it('requests entity-aware Vendor Bill approval history', () => {
    approvals.history('VENDOR_BILL', 12).subscribe();
    const request = http.expectOne(`${APP_CONFIG.apiUrl}/approvals/entity/VENDOR_BILL/12/history`);
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [] });
  });

  it('submits a Vendor Bill through the additive approval endpoint', () => {
    vendorBills.submitForApproval(12).subscribe();
    const request = http.expectOne(`${APP_CONFIG.apiUrl}/vendor-bills/12/submit-approval`);
    expect(request.request.method).toBe('POST');
    request.flush({ success: true, data: null });
  });

  it('requests entity-aware Invoice approval history', () => {
    approvals.history('INVOICE', 15).subscribe();
    const request = http.expectOne(`${APP_CONFIG.apiUrl}/approvals/entity/INVOICE/15/history`);
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [] });
  });

  it('submits an Invoice through the additive approval endpoint', () => {
    invoices.submitForApproval(15).subscribe();
    const request = http.expectOne(`${APP_CONFIG.apiUrl}/invoices/15/submit-approval`);
    expect(request.request.method).toBe('POST');
    request.flush({ success: true, data: null });
  });
});

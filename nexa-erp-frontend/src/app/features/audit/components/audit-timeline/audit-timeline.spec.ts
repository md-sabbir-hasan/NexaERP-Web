import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { PageResponse } from '../../../../core/models/page.model';
import { Subject, of, throwError } from 'rxjs';
import { AuditTimelineItem } from '../../models/audit-log.model';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditTimeline } from './audit-timeline';

const timelineItem: AuditTimelineItem = {
  id: 1,
  entityName: 'INVOICE',
  entityId: 42,
  action: 'POSTED',
  actorName: 'Amina Rahman',
  description: 'Invoice was posted',
  createdAt: '2026-07-25T12:30:00',
};

function timelineResponse(
  content: AuditTimelineItem[],
): ApiResponse<PageResponse<AuditTimelineItem>> {
  return {
    success: true,
    message: 'Success',
    data: {
      content,
      page: 0,
      size: 20,
      totalElements: content.length,
      totalPages: content.length ? 1 : 0,
      first: true,
      last: true,
    },
  };
}

describe('AuditTimeline', () => {
  let fixture: ComponentFixture<AuditTimeline>;
  let component: AuditTimeline;
  let auditService: {
    getEntityTimeline: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    auditService = {
      getEntityTimeline: vi.fn(() => of(timelineResponse([timelineItem]))),
    };

    await TestBed.configureTestingModule({
      imports: [AuditTimeline],
      providers: [{ provide: AuditLogService, useValue: auditService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditTimeline);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('entityName', 'INVOICE');
    fixture.componentRef.setInput('entityId', 42);
    fixture.detectChanges();
  });

  function openTimeline(): void {
    const button = fixture.nativeElement.querySelector('.history-btn') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
  }

  it('requests the correct entity and caps the initial page size at 20', () => {
    openTimeline();

    expect(auditService.getEntityTimeline).toHaveBeenCalledWith('INVOICE', 42, 0, 20);
  });

  it('renders the successful timeline with action badge, actor and formatted timestamp', () => {
    openTimeline();

    const text = fixture.nativeElement.textContent;
    const badge = fixture.nativeElement.querySelector('.action-pill');

    expect(text).toContain('Invoice was posted');
    expect(text).toContain('Amina Rahman');
    expect(text).toContain('2026');
    expect(badge.classList).toContain('posted');
  });

  it('renders a loading state while the request is pending', () => {
    const pending = new Subject<ApiResponse<PageResponse<AuditTimelineItem>>>();
    auditService.getEntityTimeline.mockReturnValue(pending.asObservable());

    openTimeline();

    expect(fixture.nativeElement.textContent).toContain('Loading activity');
  });

  it('renders an empty state', () => {
    auditService.getEntityTimeline.mockReturnValue(of(timelineResponse([])));

    openTimeline();

    expect(fixture.nativeElement.textContent).toContain('No activity recorded yet');
  });

  it('renders an error state and retries', () => {
    auditService.getEntityTimeline.mockReturnValueOnce(
      throwError(() => ({ error: { message: 'Timeline unavailable' } })),
    );
    openTimeline();

    expect(fixture.nativeElement.textContent).toContain('Timeline unavailable');

    auditService.getEntityTimeline.mockReturnValueOnce(of(timelineResponse([timelineItem])));
    const retry = fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement;
    retry.click();
    fixture.detectChanges();

    expect(auditService.getEntityTimeline).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Invoice was posted');
  });

  it('reloads when the entity ID changes while open', () => {
    openTimeline();

    const vendorItem: AuditTimelineItem = {
      ...timelineItem,
      id: 2,
      entityName: 'VENDOR_BILL',
      entityId: 7,
      action: 'APPROVED',
      description: 'Vendor bill was approved',
    };
    auditService.getEntityTimeline.mockReturnValueOnce(of(timelineResponse([vendorItem])));

    fixture.componentRef.setInput('entityName', 'VENDOR_BILL');
    fixture.componentRef.setInput('entityId', 7);
    fixture.detectChanges();

    expect(auditService.getEntityTimeline).toHaveBeenLastCalledWith(
      'VENDOR_BILL',
      7,
      0,
      20,
    );
    expect(fixture.nativeElement.textContent).toContain('Vendor bill was approved');
  });

  it('does not render raw old or new values or IP addresses', () => {
    const responseWithUnexpectedSensitiveFields = {
      ...timelineItem,
      oldValue: 'RAW_OLD_SECRET',
      newValue: 'RAW_NEW_SECRET',
      ipAddress: '192.0.2.10',
    } as AuditTimelineItem;
    auditService.getEntityTimeline.mockReturnValue(
      of(timelineResponse([responseWithUnexpectedSensitiveFields])),
    );

    openTimeline();

    const text = fixture.nativeElement.textContent;
    expect(text).not.toContain('RAW_OLD_SECRET');
    expect(text).not.toContain('RAW_NEW_SECRET');
    expect(text).not.toContain('192.0.2.10');
  });

  it('does not request activity until the user expands the permitted component', () => {
    expect(auditService.getEntityTimeline).not.toHaveBeenCalled();
    expect(component.open()).toBe(false);
  });
});

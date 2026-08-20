import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ReconciliationPage } from './reconciliation-page';
import { environment } from '../../../environments/environment';
import { ReconciliationReport } from '../../core/models/reconciliation.model';

describe('ReconciliationPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReconciliationPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('shows an empty state when nothing needs review', () => {
    const fixture = TestBed.createComponent(ReconciliationPage);
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reconciliation-reports/latest/needs-review`);
    req.flush([]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('everything matched');
  });

  it('renders mismatched/missing rows and retries a linked transaction', () => {
    const fixture = TestBed.createComponent(ReconciliationPage);
    fixture.detectChanges();

    const reports: ReconciliationReport[] = [
      {
        id: 1,
        runDate: '2026-08-19',
        transactionReference: 'ext-mismatch',
        transactionId: 55,
        outcome: 'MISMATCHED',
        internalAmount: 50,
        bankAmount: 45,
        discrepancy: 'Amount mismatch',
        needsReview: true,
        createdAt: '2026-08-20T00:00:00Z',
      },
    ];

    httpMock.expectOne(`${environment.apiBaseUrl}/reconciliation-reports/latest/needs-review`).flush(reports);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('ext-mismatch');
    expect(text).toContain('MISMATCHED');

    const retryButton = (fixture.nativeElement as HTMLElement).querySelector(
      'td.mat-column-actions button',
    ) as HTMLButtonElement;
    retryButton.click();

    const retryReq = httpMock.expectOne(`${environment.apiBaseUrl}/transactions/55/retry`);
    expect(retryReq.request.method).toBe('POST');
    retryReq.flush({});
  });
});

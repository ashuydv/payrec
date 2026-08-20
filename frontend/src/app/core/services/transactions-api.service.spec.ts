import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TransactionsApiService } from './transactions-api.service';
import { environment } from '../../../environments/environment';
import { PageResponse, Transaction } from '../models/transaction.model';

describe('TransactionsApiService', () => {
  let service: TransactionsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransactionsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('requests the transactions list with page/size params by default', () => {
    const emptyPage: PageResponse<Transaction> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };

    service.list({}).subscribe((result) => {
      expect(result).toEqual(emptyPage);
    });

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/transactions` && r.params.get('page') === '0' && r.params.get('size') === '20',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(emptyPage);
  });

  it('includes status and merchantId filters when provided', () => {
    const emptyPage: PageResponse<Transaction> = {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    };

    service.list({ status: 'FAILED', merchantId: 7 }).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/transactions` && r.params.get('status') === 'FAILED' && r.params.get('merchantId') === '7',
    );
    req.flush(emptyPage);
  });

  it('posts to the retry endpoint for a given transaction id', () => {
    service.retry(42).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/transactions/42/retry`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});

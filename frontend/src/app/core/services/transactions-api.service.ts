import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse, Transaction, TransactionFilter } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/transactions`;

  list(filter: TransactionFilter): Observable<PageResponse<Transaction>> {
    let params = new HttpParams()
      .set('page', filter.page ?? 0)
      .set('size', filter.size ?? 20);

    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.merchantId != null) {
      params = params.set('merchantId', filter.merchantId);
    }
    if (filter.from) {
      params = params.set('from', filter.from);
    }
    if (filter.to) {
      params = params.set('to', filter.to);
    }

    return this.http.get<PageResponse<Transaction>>(this.baseUrl, { params });
  }

  retry(id: number): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/${id}/retry`, {});
  }
}

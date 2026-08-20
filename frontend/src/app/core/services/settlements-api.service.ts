import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SettlementSummaryRow } from '../models/settlement.model';

@Injectable({ providedIn: 'root' })
export class SettlementsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/settlements`;

  summary(merchantId?: number): Observable<SettlementSummaryRow[]> {
    let params = new HttpParams();
    if (merchantId != null) {
      params = params.set('merchantId', merchantId);
    }
    return this.http.get<SettlementSummaryRow[]>(`${this.baseUrl}/summary`, { params });
  }
}

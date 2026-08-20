import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BatchJobRunResponse, ReconciliationReport } from '../models/reconciliation.model';

@Injectable({ providedIn: 'root' })
export class ReconciliationApiService {
  private readonly http = inject(HttpClient);

  latestNeedsReview(): Observable<ReconciliationReport[]> {
    return this.http.get<ReconciliationReport[]>(
      `${environment.apiBaseUrl}/reconciliation-reports/latest/needs-review`,
    );
  }

  runBatchJob(): Observable<BatchJobRunResponse> {
    return this.http.post<BatchJobRunResponse>(`${environment.apiBaseUrl}/batch/reconciliation/run`, {});
  }
}

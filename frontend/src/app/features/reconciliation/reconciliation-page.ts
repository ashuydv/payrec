import { Component, inject, signal } from '@angular/core';
import { NgClass, DecimalPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ReconciliationApiService } from '../../core/services/reconciliation-api.service';
import { TransactionsApiService } from '../../core/services/transactions-api.service';
import { ReconciliationReport } from '../../core/models/reconciliation.model';

@Component({
  selector: 'app-reconciliation-page',
  imports: [NgClass, DecimalPipe, MatTableModule, MatButtonModule, MatProgressSpinnerModule, MatSnackBarModule],
  templateUrl: './reconciliation-page.html',
  styleUrl: './reconciliation-page.scss',
})
export class ReconciliationPage {
  private readonly reconciliationApi = inject(ReconciliationApiService);
  private readonly transactionsApi = inject(TransactionsApiService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly displayedColumns = [
    'transactionReference',
    'outcome',
    'internalAmount',
    'bankAmount',
    'discrepancy',
    'runDate',
    'actions',
  ];

  protected readonly reports = signal<ReconciliationReport[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly runningJob = signal(false);
  protected readonly retryingIds = signal<Set<number>>(new Set());

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.reconciliationApi.latestNeedsReview().subscribe({
      next: (reports) => {
        this.reports.set(reports);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load reconciliation report. Is the backend running?');
        this.loading.set(false);
      },
    });
  }

  protected runBatchJob(): void {
    this.runningJob.set(true);
    this.reconciliationApi.runBatchJob().subscribe({
      next: () => {
        this.runningJob.set(false);
        this.snackBar.open('Reconciliation job triggered', 'Dismiss', { duration: 3000 });
        this.load();
      },
      error: () => {
        this.runningJob.set(false);
        this.snackBar.open('Failed to trigger reconciliation job', 'Dismiss', { duration: 4000 });
      },
    });
  }

  protected retryTransaction(report: ReconciliationReport): void {
    const transactionId = report.transactionId;
    if (transactionId == null) {
      this.snackBar.open('No transaction linked to this reference', 'Dismiss', { duration: 3000 });
      return;
    }

    this.retryingIds.update((ids) => new Set(ids).add(transactionId));
    this.transactionsApi.retry(transactionId).subscribe({
      next: () => {
        this.stopRetrying(transactionId);
        this.snackBar.open(`Retried transaction ${transactionId}`, 'Dismiss', { duration: 3000 });
      },
      error: (err) => {
        this.stopRetrying(transactionId);
        const message = err?.error?.message ?? 'Retry failed';
        this.snackBar.open(message, 'Dismiss', { duration: 4000 });
      },
    });
  }

  private stopRetrying(transactionId: number): void {
    this.retryingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(transactionId);
      return next;
    });
  }

  protected isRetrying(transactionId: number | null | undefined): boolean {
    return transactionId != null && this.retryingIds().has(transactionId);
  }

  protected outcomeChipClass(outcome: string): string {
    return outcome === 'MISMATCHED' ? 'outcome-mismatched' : 'outcome-missing';
  }
}

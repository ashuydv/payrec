import { Component, inject, signal } from '@angular/core';
import { NgClass, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { TransactionsApiService } from '../../core/services/transactions-api.service';
import { Transaction, TransactionStatus } from '../../core/models/transaction.model';

@Component({
  selector: 'app-transactions-page',
  imports: [
    NgClass,
    DecimalPipe,
    DatePipe,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  templateUrl: './transactions-page.html',
  styleUrl: './transactions-page.scss',
})
export class TransactionsPage {
  private readonly api = inject(TransactionsApiService);

  protected readonly statuses: TransactionStatus[] = ['PENDING', 'PROCESSED', 'FAILED', 'DISPUTED'];
  protected readonly displayedColumns = [
    'id',
    'merchantName',
    'amount',
    'currency',
    'status',
    'paymentType',
    'externalReference',
    'createdAt',
  ];

  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly statusFilter = signal<TransactionStatus | ''>('');
  protected readonly merchantIdFilter = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.api
      .list({
        status: this.statusFilter() || undefined,
        merchantId: this.merchantIdFilter() ?? undefined,
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.transactions.set(page.content);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Failed to load transactions. Is the backend running?');
          this.loading.set(false);
        },
      });
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected onFilterChange(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected statusChipColor(status: TransactionStatus): string {
    switch (status) {
      case 'PROCESSED':
        return 'status-processed';
      case 'FAILED':
        return 'status-failed';
      case 'DISPUTED':
        return 'status-disputed';
      default:
        return 'status-pending';
    }
  }
}

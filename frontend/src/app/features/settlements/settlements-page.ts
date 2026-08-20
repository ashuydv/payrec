import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SettlementsApiService } from '../../core/services/settlements-api.service';
import { SettlementSummaryRow } from '../../core/models/settlement.model';

@Component({
  selector: 'app-settlements-page',
  imports: [
    DecimalPipe,
    FormsModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './settlements-page.html',
  styleUrl: './settlements-page.scss',
})
export class SettlementsPage {
  private readonly api = inject(SettlementsApiService);

  protected readonly displayedColumns = ['merchantName', 'period', 'totalAmount', 'status'];
  protected readonly rows = signal<SettlementSummaryRow[]>([]);
  protected readonly merchantIdFilter = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.api.summary(this.merchantIdFilter() ?? undefined).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load settlement summary. Is the backend running?');
        this.loading.set(false);
      },
    });
  }

  protected totalAcrossRows(): number {
    return this.rows().reduce((sum, row) => sum + row.totalAmount, 0);
  }
}

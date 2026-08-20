export type ReconciliationOutcome = 'MATCHED' | 'MISMATCHED' | 'MISSING';

export interface ReconciliationReport {
  id: number;
  runDate: string;
  transactionReference: string;
  transactionId: number | null;
  outcome: ReconciliationOutcome;
  internalAmount: number | null;
  bankAmount: number | null;
  discrepancy: string | null;
  needsReview: boolean;
  createdAt: string;
}

export interface BatchJobRunResponse {
  jobExecutionId: number;
  status: string;
}

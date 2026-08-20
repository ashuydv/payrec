export type TransactionStatus = 'PENDING' | 'PROCESSED' | 'FAILED' | 'DISPUTED';
export type PaymentType = 'CARD' | 'BANK_TRANSFER';

export interface Transaction {
  id: number;
  merchantId: number;
  merchantName: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  paymentType: PaymentType;
  externalReference: string;
  createdAt: string;
  processedAt: string | null;
  retryCount: number;
  nextRetryAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionFilter {
  status?: TransactionStatus;
  merchantId?: number;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

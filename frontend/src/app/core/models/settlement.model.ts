export type SettlementStatus = 'OPEN' | 'FINALIZED';

export interface Settlement {
  id: number;
  merchantId: number;
  merchantName: string;
  period: string;
  totalAmount: number;
  status: SettlementStatus;
  version: number;
}

export interface SettlementSummaryRow {
  merchantId: number;
  merchantName: string;
  period: string;
  totalAmount: number;
  status: string;
}

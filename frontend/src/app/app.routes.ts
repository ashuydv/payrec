import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'transactions', pathMatch: 'full' },
  {
    path: 'transactions',
    loadComponent: () => import('./features/transactions/transactions-page').then((m) => m.TransactionsPage),
  },
  {
    path: 'reconciliation',
    loadComponent: () => import('./features/reconciliation/reconciliation-page').then((m) => m.ReconciliationPage),
  },
  {
    path: 'settlements',
    loadComponent: () => import('./features/settlements/settlements-page').then((m) => m.SettlementsPage),
  },
  { path: '**', redirectTo: 'transactions' },
];

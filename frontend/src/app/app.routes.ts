import { Routes } from '@angular/router';
import { adminGuard, authGuard, businessGuard, guestGuard, homeGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [homeGuard], children: [] },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/forgot-password.component').then((m) => m.ForgotPasswordComponent),
  },
  {
    path: 'reset-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/reset-password.component').then((m) => m.ResetPasswordComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/public-layout.component').then((m) => m.PublicLayoutComponent),
    children: [
      { path: 'privacy', loadComponent: () => import('./features/public/legal.component').then((m) => m.PrivacyComponent) },
      { path: 'terms', loadComponent: () => import('./features/public/legal.component').then((m) => m.TermsComponent) },
    ],
  },
  {
    path: 'app',
    canActivate: [authGuard, businessGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/business/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'devices', loadComponent: () => import('./features/business/devices.component').then((m) => m.DevicesComponent) },
      { path: 'devices/:id', loadComponent: () => import('./features/shared/device-detail.component').then((m) => m.DeviceDetailComponent) },
      { path: 'business', loadComponent: () => import('./features/business/my-business.component').then((m) => m.MyBusinessComponent) },
      { path: 'account', loadComponent: () => import('./features/shared/account.component').then((m) => m.AccountComponent) },
    ],
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent) },
      { path: 'businesses', loadComponent: () => import('./features/admin/businesses.component').then((m) => m.BusinessesComponent) },
      { path: 'businesses/new', loadComponent: () => import('./features/admin/business-wizard.component').then((m) => m.BusinessWizardComponent) },
      { path: 'businesses/:id', loadComponent: () => import('./features/admin/business-detail.component').then((m) => m.BusinessDetailComponent) },
      { path: 'users', loadComponent: () => import('./features/admin/users.component').then((m) => m.UsersComponent) },
      { path: 'devices', loadComponent: () => import('./features/admin/admin-devices.component').then((m) => m.AdminDevicesComponent) },
      { path: 'devices/:id', loadComponent: () => import('./features/shared/device-detail.component').then((m) => m.DeviceDetailComponent) },
      { path: 'account', loadComponent: () => import('./features/shared/account.component').then((m) => m.AccountComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];

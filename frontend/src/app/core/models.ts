// Espejo de los DTOs del backend (src/main/java/com/reviewtap/**/*Dtos.java).

export type UserRole = 'ADMIN' | 'BUSINESS_USER';
export type BusinessRole = 'OWNER' | 'MANAGER';
export type DeviceType = 'NFC' | 'QR' | 'NFC_QR';
export type InteractionType = 'NFC' | 'QR' | 'UNKNOWN';
export type PeriodKey = 'today' | '7d' | '30d' | 'this_month' | 'last_month' | 'custom';

export interface ApiError {
  code: string;
  message: string;
  fields?: Record<string, string>;
  status?: number;
}

export interface BusinessMembership {
  id: string;
  name: string;
  slug: string;
  timezone: string;
  active: boolean;
  role: BusinessRole;
}

export interface Me {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  businesses: BusinessMembership[];
}

export interface Business {
  id: string;
  name: string;
  slug: string;
  googleReviewUrl: string | null;
  logoUrl: string | null;
  address: string | null;
  phone: string | null;
  timezone: string;
  active: boolean;
  createdAt: string;
  deviceCount: number;
}

export interface BusinessCreateRequest {
  name: string;
  googleReviewUrl?: string | null;
  timezone?: string;
  address?: string | null;
  phone?: string | null;
  ownerEmail?: string | null;
  ownerFirstName?: string | null;
  ownerLastName?: string | null;
  ownerPassword?: string | null;
}

export interface OwnerResult {
  userId: string;
  email: string;
  created: boolean;
  generatedPassword: string | null;
}

export interface BusinessCreated {
  business: Business;
  owner: OwnerResult | null;
}

export interface BusinessUpdateRequest {
  name: string;
  googleReviewUrl: string | null;
  logoUrl: string | null;
  address: string | null;
  phone: string | null;
  timezone: string;
}

export interface BusinessProfileUpdateRequest {
  name: string;
  logoUrl: string | null;
  address: string | null;
  phone: string | null;
  timezone: string;
}

export interface Member {
  userId: string;
  email: string;
  fullName: string;
  enabled: boolean;
  role: BusinessRole;
  since: string;
}

export interface MemberAddRequest {
  email: string;
  role: BusinessRole;
  firstName?: string | null;
  lastName?: string | null;
  password?: string | null;
}

export interface MemberAdded {
  member: Member;
  userCreated: boolean;
  generatedPassword: string | null;
}

export interface Device {
  id: string;
  businessId: string;
  businessName: string;
  publicCode: string;
  name: string;
  locationDescription: string | null;
  type: DeviceType;
  active: boolean;
  nfcUrl: string;
  qrUrl: string;
  interactionsLast30Days: number;
  createdAt: string;
}

export interface DeviceRequest {
  name: string;
  locationDescription: string | null;
  type: DeviceType;
}

export interface UserBusinessRef {
  id: string;
  name: string;
  role: BusinessRole;
}

export interface AppUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  businesses: UserBusinessRef[];
}

export interface UserCreateRequest {
  email: string;
  password?: string | null;
  firstName: string;
  lastName?: string | null;
  role: UserRole;
}

export interface UserCreated {
  user: AppUser;
  generatedPassword: string | null;
}

export interface PeriodInfo {
  key: string;
  from: string;
  to: string;
  timezone: string;
}

export interface Counts {
  total: number;
  nfc: number;
  qr: number;
  unknown: number;
}

export interface Summary {
  period: PeriodInfo;
  previousPeriod: PeriodInfo;
  current: Counts;
  previous: Counts;
  changePercent: number | null;
  topDevice: { id: string; name: string; total: number } | null;
}

export interface TimelinePoint {
  date: string;
  total: number;
  nfc: number;
  qr: number;
}

export interface Timeline {
  period: PeriodInfo;
  points: TimelinePoint[];
}

export interface DeviceStatsRow {
  deviceId: string;
  name: string;
  location: string | null;
  active: boolean;
  nfc: number;
  qr: number;
  unknown: number;
  total: number;
}

export interface DeviceStats {
  period: PeriodInfo;
  devices: DeviceStatsRow[];
}

export interface RecentRow {
  createdAt: string;
  deviceId: string;
  deviceName: string;
  location: string | null;
  type: InteractionType;
}

export interface AdminSummary {
  period: PeriodInfo;
  previousPeriod: PeriodInfo;
  current: Counts;
  previous: Counts;
  changePercent: number | null;
  businesses: number;
  activeBusinesses: number;
  devices: number;
  activeDevices: number;
  topBusinesses: { id: string; name: string; total: number }[];
  timeline: TimelinePoint[];
}

export interface PeriodQuery {
  period: PeriodKey;
  from?: string;
  to?: string;
}

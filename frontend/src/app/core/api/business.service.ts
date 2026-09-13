import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  Business,
  BusinessCreateRequest,
  BusinessCreated,
  BusinessProfileUpdateRequest,
  BusinessUpdateRequest,
  Member,
  MemberAddRequest,
  MemberAdded,
} from '../models';

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private readonly http = inject(HttpClient);

  list(): Promise<Business[]> {
    return firstValueFrom(this.http.get<Business[]>('/api/businesses'));
  }

  get(id: string): Promise<Business> {
    return firstValueFrom(this.http.get<Business>(`/api/businesses/${id}`));
  }

  create(req: BusinessCreateRequest): Promise<BusinessCreated> {
    return firstValueFrom(this.http.post<BusinessCreated>('/api/businesses', req));
  }

  update(id: string, req: BusinessUpdateRequest): Promise<Business> {
    return firstValueFrom(this.http.put<Business>(`/api/businesses/${id}`, req));
  }

  updateProfile(id: string, req: BusinessProfileUpdateRequest): Promise<Business> {
    return firstValueFrom(this.http.patch<Business>(`/api/businesses/${id}/profile`, req));
  }

  setActive(id: string, active: boolean): Promise<Business> {
    return firstValueFrom(this.http.patch<Business>(`/api/businesses/${id}/status`, { active }));
  }

  members(id: string): Promise<Member[]> {
    return firstValueFrom(this.http.get<Member[]>(`/api/businesses/${id}/members`));
  }

  addMember(id: string, req: MemberAddRequest): Promise<MemberAdded> {
    return firstValueFrom(this.http.post<MemberAdded>(`/api/businesses/${id}/members`, req));
  }

  removeMember(id: string, userId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/businesses/${id}/members/${userId}`));
  }
}

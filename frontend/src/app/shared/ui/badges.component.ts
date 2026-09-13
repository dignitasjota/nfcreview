import { Component, input } from '@angular/core';
import { DeviceType, InteractionType } from '../../core/models';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class.badge-success]="active()" [class.badge-muted]="!active()"><span class="dot"></span>{{ active() ? 'Activo' : 'Inactivo' }}</span>`,
})
export class StatusBadgeComponent {
  readonly active = input.required<boolean>();
}

export const DEVICE_TYPE_LABEL: Record<DeviceType, string> = { NFC: 'NFC', QR: 'QR', NFC_QR: 'NFC + QR' };

@Component({
  selector: 'app-device-type-badge',
  template: `<span class="badge badge-info">{{ label }}</span>`,
})
export class DeviceTypeBadgeComponent {
  readonly type = input.required<DeviceType>();
  get label() {
    return DEVICE_TYPE_LABEL[this.type()] ?? this.type();
  }
}

@Component({
  selector: 'app-interaction-badge',
  template: `<span class="badge" [class.badge-nfc]="type() === 'NFC'" [class.badge-qr]="type() === 'QR'" [class.badge-muted]="type() === 'UNKNOWN'">{{ type() === 'UNKNOWN' ? 'Desconocido' : type() }}</span>`,
})
export class InteractionBadgeComponent {
  readonly type = input.required<InteractionType>();
}

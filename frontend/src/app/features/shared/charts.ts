import { ChartConfiguration } from 'chart.js';
import { TimelinePoint } from '../../core/models';

const NFC = '#0f766e';
const QR = '#f59e0b';
const OTHER = '#94a3b8';

function shortDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

/** Barras apiladas NFC/QR por día. */
export function timelineChart(points: TimelinePoint[]): ChartConfiguration<'bar'> {
  const other = points.map((p) => Math.max(0, p.total - p.nfc - p.qr));
  const datasets: ChartConfiguration<'bar'>['data']['datasets'] = [
    { label: 'NFC', data: points.map((p) => p.nfc), backgroundColor: NFC, borderRadius: 3, stack: 'a' },
    { label: 'QR', data: points.map((p) => p.qr), backgroundColor: QR, borderRadius: 3, stack: 'a' },
  ];
  if (other.some((v) => v > 0)) {
    datasets.push({ label: 'Otros', data: other, backgroundColor: OTHER, borderRadius: 3, stack: 'a' });
  }
  return {
    type: 'bar',
    data: { labels: points.map((p) => shortDate(p.date)), datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, usePointStyle: true } } },
      scales: {
        x: { stacked: true, grid: { display: false }, ticks: { maxTicksLimit: 12, maxRotation: 0 } },
        y: { stacked: true, beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#eef2f6' } },
      },
    },
  };
}

export function distributionChart(nfc: number, qr: number, unknown: number): ChartConfiguration<'doughnut'> {
  const labels = ['NFC', 'QR'];
  const data = [nfc, qr];
  const colors = [NFC, QR];
  if (unknown > 0) {
    labels.push('Otros');
    data.push(unknown);
    colors.push(OTHER);
  }
  return {
    type: 'doughnut',
    data: { labels, datasets: [{ data, backgroundColor: colors, borderWidth: 0, hoverOffset: 4 }] },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '68%',
      plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, usePointStyle: true } } },
    },
  };
}

export function percent(part: number, total: number): string {
  return total === 0 ? '0 %' : `${Math.round((part / total) * 100)} %`;
}

export function changeText(change: number | null, previousTotal: number): string {
  if (change === null) return previousTotal === 0 ? 'Sin datos en el periodo anterior' : '—';
  const sign = change > 0 ? '+' : '';
  return `${sign}${change.toLocaleString('es-ES', { maximumFractionDigits: 1 })} % respecto al periodo anterior`;
}

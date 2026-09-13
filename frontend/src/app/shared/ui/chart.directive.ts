import { Directive, ElementRef, OnDestroy, effect, inject, input } from '@angular/core';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  ChartType,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';

Chart.register(LineController, BarController, DoughnutController, LineElement, BarElement, PointElement, ArcElement,
  CategoryScale, LinearScale, Filler, Legend, Tooltip);
Chart.defaults.font.family = getComputedStyle(document.documentElement).getPropertyValue('--font') || 'system-ui';
Chart.defaults.color = '#64748b';

/** Envoltorio mínimo sobre Chart.js: `<canvas [appChart]="config">`. Se destruye con el elemento. */
@Directive({ selector: 'canvas[appChart]' })
export class ChartDirective implements OnDestroy {
  readonly appChart = input.required<ChartConfiguration<ChartType> | null>();
  private readonly canvas = inject<ElementRef<HTMLCanvasElement>>(ElementRef);
  private chart: Chart | null = null;
  private currentType: ChartType | null = null;

  constructor() {
    effect(() => {
      const config = this.appChart();
      if (!config) {
        this.chart?.destroy();
        this.chart = null;
        return;
      }
      if (this.chart && this.currentType === config.type) {
        this.chart.data = config.data;
        this.chart.options = config.options ?? {};
        this.chart.update();
      } else {
        this.chart?.destroy();
        this.chart = new Chart(this.canvas.nativeElement, config);
        this.currentType = config.type;
      }
    });
  }

  ngOnDestroy() {
    this.chart?.destroy();
  }
}

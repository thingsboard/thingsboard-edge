///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  LatestChartDataItem,
  LatestChartLegendItem,
  LatestChartSettings,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle, textStyle } from '@shared/models/widget-settings.models';
import { TbLatestChart } from '@home/components/widget/lib/chart/latest-chart';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TranslateService } from '@ngx-translate/core';
import { LegendPosition } from '@shared/models/widget.models';

export interface LatestChartComponentCallbacks {
  createChart: (chartShape: ElementRef<HTMLElement>, renderer: Renderer2) => TbLatestChart<LatestChartSettings>;
  onItemClick?: ($event: Event, item: LatestChartDataItem) => void;
}

@Component({
  selector: 'tb-latest-chart',
  templateUrl: './latest-chart.component.html',
  styleUrls: ['./latest-chart.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class LatestChartComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('chartContent', {static: false})
  chartContent: ElementRef<HTMLElement>;

  @ViewChild('chartShape', {static: false})
  chartShape: ElementRef<HTMLElement>;

  @ViewChild('chartLegend', {static: false})
  chartLegend: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  @Input()
  callbacks: LatestChartComponentCallbacks;

  @Input()
  settings: LatestChartWidgetSettings;

  showLegend: boolean;
  legendClass: string;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  get legendItems(): LatestChartLegendItem[] {
    return this.latestChart ? this.latestChart.getLegendItems() : [];
  }

  legendLabelStyle: ComponentStyle;
  legendValueStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;
  disabledLegendValueStyle: ComponentStyle;

  private shapeResize$: ResizeObserver;
  private legendHorizontal: boolean;

  private latestChart: TbLatestChart<LatestChartSettings>;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private widgetComponent: WidgetComponent,
              private renderer: Renderer2,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.showLegend = this.settings.showLegend;

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    if (this.showLegend) {
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendHorizontal = [LegendPosition.left, LegendPosition.right].includes(this.settings.legendPosition);
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
      this.legendValueStyle = textStyle(this.settings.legendValueFont);
      this.disabledLegendValueStyle = textStyle(this.settings.legendValueFont);
      this.legendValueStyle.color = this.settings.legendValueColor;
    }
  }

  ngAfterViewInit() {
    this.latestChart = this.callbacks.createChart(this.chartShape, this.renderer);
    this.latestChart.onItemClick(this.callbacks.onItemClick);
    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.chartContent.nativeElement);
    this.onResize();
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.latestChart) {
      this.latestChart.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    if (this.latestChart) {
      this.latestChart.update();
    }
    if (this.showLegend) {
      this.cd.detectChanges();
      if (this.legendHorizontal) {
        setTimeout(() => {
          this.onResize();
        });
      }
    }
  }

  public onLegendItemEnter(item: LatestChartLegendItem) {
    if (!item.total && item.hasValue) {
      this.latestChart.keyEnter(item.dataKey);
    }
  }

  public onLegendItemLeave(item: LatestChartLegendItem) {
    if (!item.total && item.hasValue) {
      this.latestChart.keyLeave(item.dataKey);
    }
  }

  public toggleLegendItem(item: LatestChartLegendItem) {
    if (!item.total && item.hasValue) {
      this.latestChart.toggleKey(item.dataKey);
    }
  }

  private onResize() {
    if (this.legendHorizontal) {
      this.renderer.setStyle(this.chartShape.nativeElement, 'max-width', null);
      this.renderer.setStyle(this.chartShape.nativeElement, 'min-width', null);
      this.renderer.setStyle(this.chartLegend.nativeElement, 'flex', null);
    }
    const shapeWidth = this.chartShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.chartShape.nativeElement.getBoundingClientRect().height;
    const size = Math.min(shapeWidth, shapeHeight);
    if (this.legendHorizontal) {
      this.renderer.setStyle(this.chartShape.nativeElement, 'max-width', `${size}px`);
      this.renderer.setStyle(this.chartShape.nativeElement, 'min-width', `${size}px`);
      this.renderer.setStyle(this.chartLegend.nativeElement, 'flex', '1');
    }
  }

}

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
import { WidgetContext } from '@home/models/widget-component.models';
import { backgroundStyle, ComponentStyle, overlayStyle, textStyle } from '@shared/models/widget-settings.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  barChartWithLabelsDefaultSettings,
  barChartWithLabelsTimeSeriesKeySettings,
  barChartWithLabelsTimeSeriesSettings,
  BarChartWithLabelsWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import { DataKey } from '@shared/models/widget.models';
import { WidgetComponent } from '@home/components/widget/widget.component';

@Component({
  selector: 'tb-bar-chart-with-labels-widget',
  templateUrl: './bar-chart-with-labels-widget.component.html',
  styleUrls: ['./bar-chart-with-labels-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class BarChartWithLabelsWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('chartShape', {static: false})
  chartShape: ElementRef<HTMLElement>;

  settings: BarChartWithLabelsWidgetSettings;

  @Input()
  ctx: WidgetContext;

  showLegend: boolean;
  legendClass: string;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  legendKeys: DataKey[];
  legendLabelStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;

  private timeSeriesChart: TbTimeSeriesChart;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.barChartWidget = this;
    this.settings = {...barChartWithLabelsDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.showLegend = this.settings.showLegend;

    if (this.showLegend) {
      this.legendKeys = [];
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
    }
    const barSettings = barChartWithLabelsTimeSeriesKeySettings(this.settings, this.ctx.decimals);
    for (const datasource of this.ctx.datasources) {
      const dataKeys = datasource.dataKeys;
      for (const dataKey of dataKeys) {
        dataKey.settings = barSettings;
        if (this.showLegend) {
          this.legendKeys.push(dataKey);
        }
      }
    }
  }

  ngAfterViewInit() {
    const settings = barChartWithLabelsTimeSeriesSettings(this.settings);
    this.timeSeriesChart = new TbTimeSeriesChart(this.ctx, settings, this.chartShape.nativeElement, this.renderer);
  }

  ngOnDestroy() {
    if (this.timeSeriesChart) {
      this.timeSeriesChart.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    if (this.timeSeriesChart) {
      this.timeSeriesChart.update();
    }
  }

  public onLegendKeyEnter(key: DataKey) {
    this.timeSeriesChart.keyEnter(key);
  }

  public onLegendKeyLeave(key: DataKey) {
    this.timeSeriesChart.keyLeave(key);
  }

  public toggleLegendKey(key: DataKey) {
    this.timeSeriesChart.toggleKey(key);
  }
}

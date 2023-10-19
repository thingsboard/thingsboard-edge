///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
  ViewChild
} from '@angular/core';
import {
  aggregatedValueCardDefaultSettings,
  AggregatedValueCardKeyPosition,
  AggregatedValueCardValue,
  AggregatedValueCardWidgetSettings,
  computeAggregatedCardValue,
  getTsValueByLatestDataKey
} from '@home/components/widget/lib/cards/aggregated-value-card.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import {
  backgroundStyle,
  ComponentStyle,
  DateFormatProcessor,
  getDataKey,
  getLatestSingleTsValue,
  overlayStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import { TbFlotKeySettings, TbFlotSettings } from '@home/components/widget/lib/flot-widget.models';
import { DataKey } from '@shared/models/widget.models';
import { formatNumberValue, formatValue, isDefined } from '@core/utils';
import { map } from 'rxjs/operators';
import { ResizeObserver } from '@juggle/resize-observer';

const valuesLayoutHeight = 66;
const valuesLayoutVerticalPadding = 16;

@Component({
  selector: 'tb-aggregated-value-card-widget',
  templateUrl: './aggregated-value-card-widget.component.html',
  styleUrls: ['./aggregated-value-card-widget.component.scss']
})
export class AggregatedValueCardWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('chartElement', {static: false}) chartElement: ElementRef;

  @ViewChild('valueCardValues', {static: false})
  valueCardValues: ElementRef<HTMLElement>;

  @ViewChild('valueCardValueContainer', {static: false})
  valueCardValueContainer: ElementRef<HTMLElement>;

  aggregatedValueCardKeyPosition = AggregatedValueCardKeyPosition;

  settings: AggregatedValueCardWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showSubtitle = true;
  subtitle$: Observable<string>;
  subtitleStyle: ComponentStyle = {};
  subtitleColor: string;

  showValues = false;

  values: {[key: string]: AggregatedValueCardValue} = {};

  showChart = true;

  showDate = true;
  dateFormat: DateFormatProcessor;
  dateStyle: ComponentStyle = {};
  dateColor: string;

  backgroundStyle: ComponentStyle = {};
  overlayStyle: ComponentStyle = {};

  private flot: TbFlot;
  private flotDataKey: DataKey;

  private lastUpdateTs: number;

  tickMin$: Observable<string>;
  tickMax$: Observable<string>;

  private panelResize$: ResizeObserver;

  constructor(private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.aggregatedValueCardWidget = this;
    this.settings = {...aggregatedValueCardDefaultSettings, ...this.ctx.settings};
    this.showSubtitle = this.settings.showSubtitle;
    const subtitle = this.settings.subtitle;
    this.subtitle$ = this.ctx.registerLabelPattern(subtitle, this.subtitle$);
    this.subtitleStyle = textStyle(this.settings.subtitleFont, '0.25px');
    this.subtitleColor =  this.settings.subtitleColor;

    const dataKey = getDataKey(this.ctx.defaultSubscription.datasources);
    if (dataKey?.name && this.ctx.defaultSubscription.firstDatasource?.latestDataKeys?.length) {
      const dataKeys = this.ctx.defaultSubscription.firstDatasource?.latestDataKeys;
      for (const position of Object.keys(AggregatedValueCardKeyPosition)) {
        const value = computeAggregatedCardValue(dataKeys, dataKey?.name, AggregatedValueCardKeyPosition[position]);
        if (value) {
          this.values[position] = value;
        }
      }
      this.showValues = !!Object.keys(this.values).length;
    }

    this.showChart = this.settings.showChart;
    if (this.showChart) {
      if (this.ctx.defaultSubscription.firstDatasource?.dataKeys?.length) {
        this.flotDataKey = this.ctx.defaultSubscription.firstDatasource?.dataKeys[0];
        this.flotDataKey.settings = {
          fillLines: false,
          showLines: true,
          lineWidth: 2
        } as TbFlotKeySettings;
      }
    }

    this.showDate = this.settings.showDate;
    this.dateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.dateFormat);
    this.dateStyle = textStyle(this.settings.dateFont,  '0.25px');
    this.dateColor = this.settings.dateColor;

    this.backgroundStyle = backgroundStyle(this.settings.background);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
  }

  ngAfterViewInit(): void {
    if (this.showChart) {
      const settings = {
        shadowSize: 0,
        enableSelection: false,
        smoothLines: false,
        grid: {
          tickColor: 'rgba(0,0,0,0.12)',
          horizontalLines: true,
          verticalLines: false,
          outlineWidth: 0,
          minBorderMargin: 0,
          margin: 0
        },
        yaxis: {
          showLabels: false,
          tickGenerator: 'return [(axis.max + axis.min) / 2];'
        },
        xaxis: {
          showLabels: false
        }
      } as TbFlotSettings;
      this.flot = new TbFlot(this.ctx, 'line', $(this.chartElement.nativeElement), settings);
      this.tickMin$ = this.flot.yMin$.pipe(
        map((value) => formatValue(value, (this.flotDataKey?.decimals || this.ctx.decimals))
      ));
      this.tickMax$ = this.flot.yMax$.pipe(
        map((value) => formatValue(value, (this.flotDataKey?.decimals || this.ctx.decimals))
        ));
    }
    if (this.settings.autoScale && this.showValues) {
      this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'height', valuesLayoutHeight + 'px');
      this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onValueCardValuesResize();
      });
      this.panelResize$.observe(this.valueCardValues.nativeElement);
      this.onValueCardValuesResize();
    }
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getLatestSingleTsValue(this.ctx.data);
    let ts;
    if (tsValue) {
      ts = tsValue[0];
    }

    if (this.showChart) {
      this.flot.update();
    }

    this.updateLastUpdateTs(ts);
    this.cd.detectChanges();
  }

  public onLatestDataUpdated() {
    if (this.showValues) {
      for (const aggValue of Object.values(this.values)) {
        const tsValue = getTsValueByLatestDataKey(this.ctx.latestData, aggValue.key);
        let ts;
        let value;
        if (tsValue) {
          ts = tsValue[0];
          value = tsValue[1];
          aggValue.value = formatValue(value, (aggValue.key.decimals || this.ctx.decimals), null, false);
        } else {
          aggValue.value = 'N/A';
        }
        const numeric = formatNumberValue(value, (aggValue.key.decimals || this.ctx.decimals));
        aggValue.color.update(numeric);
        if (aggValue.showArrow && isDefined(numeric)) {
          aggValue.upArrow = numeric > 0;
          aggValue.downArrow = numeric < 0;
        } else {
          aggValue.upArrow = aggValue.downArrow = false;
        }
        this.updateLastUpdateTs(ts);
      }
      this.cd.detectChanges();
    }
  }

  public onResize() {
    if (this.showChart) {
      this.flot.resize();
    }
  }

  public onEditModeChanged() {
    if (this.showChart) {
      this.flot.checkMouseEvents();
    }
  }

  public onDestroy() {
    if (this.showChart) {
      this.flot.destroy();
    }
  }

  private updateLastUpdateTs(ts: number) {
    if (ts && (!this.lastUpdateTs || ts > this.lastUpdateTs)) {
      this.lastUpdateTs = ts;
      this.dateFormat.update(ts);
    }
  }

  private onValueCardValuesResize() {
    const panelWidth = this.valueCardValues.nativeElement.getBoundingClientRect().width;
    const panelHeight = this.valueCardValues.nativeElement.getBoundingClientRect().height - valuesLayoutVerticalPadding;
    const targetWidth = panelWidth;
    const minAspect = 0.25;
    const aspect = Math.min(panelHeight / targetWidth, minAspect);
    const targetHeight = targetWidth * aspect;
    const scale = targetHeight / valuesLayoutHeight;
    const width = targetWidth / scale;
    this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.valueCardValueContainer.nativeElement, 'transform', `scale(${scale})`);
  }

}

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
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { formatValue, isDefinedAndNotNull, isNumeric } from '@core/utils';
import { DatePipe } from '@angular/common';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  getDataKey,
  getSingleTsValue,
  overlayStyle,
  resolveCssSize,
  textStyle
} from '@shared/models/widget-settings.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { progressBarDefaultSettings, ProgressBarLayout, ProgressBarWidgetSettings } from './progress-bar-widget.models';
import { ResizeObserver } from '@juggle/resize-observer';

const defaultLayoutHeight = 80;
const simplifiedLayoutHeight = 75;
const defaultAspect = defaultLayoutHeight / 150;
const simplifiedAspect = simplifiedLayoutHeight / 150;

@Component({
  selector: 'tb-progress-bar-widget',
  templateUrl: './progress-bar-widget.component.html',
  styleUrls: ['./progress-bar-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ProgressBarWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('progressBarPanel', {static: true})
  progressBarPanel: ElementRef<HTMLElement>;

  @ViewChild('progressBarContainer', {static: true})
  progressBarContainer: ElementRef<HTMLElement>;

  progressBarLayout = ProgressBarLayout;

  settings: ProgressBarWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: ProgressBarLayout;

  layoutClass = '';

  showTitleValueRow = true;
  titleValueRowClass = '';

  showValue = true;
  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  barWidth = '0%';
  barColor: ColorProcessor;

  showTicks = true;
  ticksStyle: ComponentStyle = {};

  value: number;

  backgroundStyle: ComponentStyle = {};
  overlayStyle: ComponentStyle = {};

  progressBarPanelResize$: ResizeObserver;

  private decimals = 0;
  private units = '';

  constructor(private date: DatePipe,
              private widgetComponent: WidgetComponent,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.progressBarWidget = this;
    this.settings = {...progressBarDefaultSettings, ...this.ctx.settings};

    this.decimals = this.ctx.decimals;
    this.units = this.ctx.units;
    const dataKey = getDataKey(this.ctx.datasources);
    if (isDefinedAndNotNull(dataKey?.decimals)) {
      this.decimals = dataKey.decimals;
    }
    if (dataKey?.units) {
      this.units = dataKey.units;
    }

    this.layout = this.settings.layout;

    this.showValue = this.settings.showValue;
    this.valueStyle = textStyle(this.settings.valueFont,  '0.1px');
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showTitleValueRow = this.showValue ||
      (this.layout === ProgressBarLayout.simplified && this.widgetComponent.dashboardWidget.showWidgetTitlePanel);

    this.layoutClass = (this.layout === ProgressBarLayout.simplified || !this.widgetComponent.dashboardWidget.showWidgetTitlePanel)
      ? 'simplified' : '';

    this.titleValueRowClass = (this.layout === ProgressBarLayout.simplified &&
      !this.widgetComponent.dashboardWidget.showWidgetTitlePanel) ? 'flex-end' : '';

    this.barColor = ColorProcessor.fromSettings(this.settings.barColor);

    this.showTicks = this.settings.showTicks && this.layout === ProgressBarLayout.default;
    if (this.showTicks) {
      this.ticksStyle = textStyle(this.settings.ticksFont,  '0.1px');
      this.ticksStyle.color = this.settings.ticksColor;
    }

    this.backgroundStyle = backgroundStyle(this.settings.background);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
  }

  ngAfterViewInit() {
    if (this.settings.autoScale) {
      this.renderer.setStyle(this.progressBarContainer.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.progressBarContainer.nativeElement, 'position', 'absolute');
      this.progressBarPanelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.progressBarPanelResize$.observe(this.progressBarPanel.nativeElement);
      this.onResize();
    }
  }

  ngOnDestroy() {
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    this.value = 0;
    if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
      this.value = tsValue[1];
      this.valueText = formatValue(this.value, this.decimals, this.units, true);
    } else {
      this.valueText = 'N/A';
    }
    this.valueColor.update(this.value);
    this.barColor.update(this.value);
    const range = this.settings.tickMax - this.settings.tickMin;
    this.barWidth = `${(this.value / range) * 100}%`;
    this.cd.detectChanges();
  }

  private onResize() {
    const paddingLeft = getComputedStyle(this.progressBarPanel.nativeElement).paddingLeft;
    const paddingRight = getComputedStyle(this.progressBarPanel.nativeElement).paddingRight;
    const paddingTop = getComputedStyle(this.progressBarPanel.nativeElement).paddingTop;
    const paddingBottom = getComputedStyle(this.progressBarPanel.nativeElement).paddingBottom;
    const pLeft = resolveCssSize(paddingLeft)[0];
    const pRight = resolveCssSize(paddingRight)[0];
    const pTop = resolveCssSize(paddingTop)[0];
    const pBottom = resolveCssSize(paddingBottom)[0];
    const panelWidth = this.progressBarPanel.nativeElement.getBoundingClientRect().width - (pLeft + pRight);
    const panelHeight = this.progressBarPanel.nativeElement.getBoundingClientRect().height - (pTop + pBottom);
    const defaultLayout = this.layout === ProgressBarLayout.default && this.widgetComponent.dashboardWidget.showWidgetTitlePanel;
    let minAspect = defaultLayout ? defaultAspect : simplifiedAspect;
    let layoutHeight = defaultLayout ? defaultLayoutHeight : simplifiedLayoutHeight;
    if (!this.showTitleValueRow) {
      minAspect -= (40 / 150);
      layoutHeight -= 40;
    }
    if (!this.showTicks) {
      minAspect -= (10 / 150);
      layoutHeight -= 10;
    }
    const aspect = Math.min(panelHeight / panelWidth, minAspect);
    const targetWidth = panelWidth;
    const targetHeight = targetWidth * aspect;
    const scale = targetHeight / layoutHeight;
    const width = targetWidth / scale;
    const height = panelHeight / scale;
    this.renderer.setStyle(this.progressBarContainer.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.progressBarContainer.nativeElement, 'height', height + 'px');
    this.renderer.setStyle(this.progressBarContainer.nativeElement, 'transform', `scale(${scale})`);
  }

}

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
import { WidgetContext } from '@home/models/widget-component.models';
import { formatValue, isDefinedAndNotNull, isNumeric } from '@core/utils';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  DateFormatProcessor,
  getDataKey,
  getLabel,
  getSingleTsValue,
  iconStyle,
  overlayStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { valueCardDefaultSettings, ValueCardLayout, ValueCardWidgetSettings } from './value-card-widget.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { Observable } from 'rxjs';
import { ResizeObserver } from '@juggle/resize-observer';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

const squareLayoutSize = 160;
const squareLayoutPadding = 48;
const horizontalLayoutHeight = 80;

@Component({
  selector: 'tb-value-card-widget',
  templateUrl: './value-card-widget.component.html',
  styleUrls: ['./value-card-widget.component.scss']
})
export class ValueCardWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('valueCardPanel', {static: false})
  valueCardPanel: ElementRef<HTMLElement>;

  @ViewChild('valueCardContent', {static: false})
  valueCardContent: ElementRef<HTMLElement>;

  settings: ValueCardWidgetSettings;

  valueCardLayout = ValueCardLayout;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: ValueCardLayout;
  showIcon = true;
  icon = '';
  iconStyle: ComponentStyle = {};
  iconColor: ColorProcessor;

  showLabel = true;
  label$: Observable<string>;
  labelStyle: ComponentStyle = {};
  labelColor: ColorProcessor;

  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  showDate = true;
  dateFormat: DateFormatProcessor;
  dateStyle: ComponentStyle = {};
  dateColor: ColorProcessor;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  private panelResize$: ResizeObserver;

  private horizontal = false;
  private decimals = 0;
  private units = '';

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private widgetComponent: WidgetComponent,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const params = this.widgetComponent.typeParameters as any;
    this.horizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.ctx.$scope.valueCardWidget = this;
    this.settings = {...valueCardDefaultSettings(this.horizontal), ...this.ctx.settings};

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

    this.showIcon = this.settings.showIcon;
    this.icon = this.settings.icon;
    this.iconStyle = iconStyle(this.settings.iconSize, this.settings.iconSizeUnit );
    this.iconColor = ColorProcessor.fromSettings(this.settings.iconColor);

    this.showLabel = this.settings.showLabel;
    const label = getLabel(this.ctx.datasources);
    this.label$ = this.ctx.registerLabelPattern(label, this.label$);
    this.labelStyle = textStyle(this.settings.labelFont);
    this.labelColor =  ColorProcessor.fromSettings(this.settings.labelColor);
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showDate = this.settings.showDate;
    this.dateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.dateFormat);
    this.dateStyle = textStyle(this.settings.dateFont);
    this.dateColor = ColorProcessor.fromSettings(this.settings.dateColor);

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
  }

  public ngAfterViewInit() {
    if (this.settings.autoScale) {
      if (!this.horizontal) {
        this.renderer.setStyle(this.valueCardContent.nativeElement, 'width', squareLayoutSize + 'px');
      }
      const height = this.horizontal ? horizontalLayoutHeight : squareLayoutSize;
      this.renderer.setStyle(this.valueCardContent.nativeElement, 'height', height + 'px');
      this.renderer.setStyle(this.valueCardContent.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.valueCardContent.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.panelResize$.observe(this.valueCardPanel.nativeElement);
      this.onResize();
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
    const tsValue = getSingleTsValue(this.ctx.data);
    let ts;
    let value;
    if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
      ts = tsValue[0];
      value = tsValue[1];
      this.valueText = formatValue(value, this.decimals, this.units, false);
    } else {
      this.valueText = 'N/A';
    }
    this.dateFormat.update(ts);
    this.iconColor.update(value);
    this.labelColor.update(value);
    this.valueColor.update(value);
    this.dateColor.update(value);
    this.cd.detectChanges();
  }

  private onResize() {
    const panelWidth = this.valueCardPanel.nativeElement.getBoundingClientRect().width - squareLayoutPadding;
    const panelHeight = this.valueCardPanel.nativeElement.getBoundingClientRect().height - (this.horizontal ? 0 : squareLayoutPadding);
    let scale: number;
    if (!this.horizontal) {
      const size = Math.min(panelWidth, panelHeight);
      scale = size / squareLayoutSize;
    } else {
      const targetWidth = panelWidth;
      const aspect = Math.min(panelHeight / targetWidth, 0.25);
      const targetHeight = targetWidth * aspect;
      scale = targetHeight / horizontalLayoutHeight;
      const width = targetWidth / scale;
      this.renderer.setStyle(this.valueCardContent.nativeElement, 'width', width + 'px');
    }
    this.renderer.setStyle(this.valueCardContent.nativeElement, 'transform', `scale(${scale})`);
  }
}

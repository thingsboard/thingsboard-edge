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
import { formatValue } from '@core/utils';
import {
  ColorProcessor,
  ComponentStyle,
  getSingleTsValue,
  iconStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import {
  CountCardLayout,
  countDefaultSettings,
  CountWidgetSettings
} from '@home/components/widget/lib/count/count-widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ResizeObserver } from '@juggle/resize-observer';
import { UtilsService } from '@core/services/utils.service';

const layoutHeight = 36;
const layoutHeightWithTitle = 60;
const layoutPadding = 24;

@Component({
  selector: 'tb-count-widget',
  templateUrl: './count-widget.component.html',
  styleUrls: ['./count-widget.component.scss']
})
export class CountWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('countPanel', {static: false})
  countPanel: ElementRef<HTMLElement>;

  @ViewChild('countPanelContent', {static: false})
  countPanelContent: ElementRef<HTMLElement>;

  settings: CountWidgetSettings;

  countCardLayout = CountCardLayout;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  @coerceBoolean()
  @Input()
  alarmElseEntity: boolean;

  layout: CountCardLayout;

  showLabel = true;
  label: string;
  labelStyle: ComponentStyle = {};
  labelColor: ColorProcessor;

  showIcon = true;
  icon = '';
  iconStyle: ComponentStyle = {};
  iconColor: ColorProcessor;

  showIconBackground = true;
  iconBackgroundSize: string;
  iconBackgroundStyle: ComponentStyle = {};
  iconBackgroundColor: ColorProcessor;

  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  showChevron = false;
  chevronStyle: ComponentStyle = {};

  hasCardClickAction = false;

  private panelResize$: ResizeObserver;
  private hasTitle = false;

  constructor(private renderer: Renderer2,
              private utils: UtilsService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.countWidget = this;
    this.settings = {...countDefaultSettings(this.alarmElseEntity), ...this.ctx.settings};

    this.layout = this.settings.layout;

    this.showLabel = this.settings.showLabel;
    this.label = this.utils.customTranslation(this.settings.label, this.settings.label);
    this.labelStyle = textStyle(this.settings.labelFont);
    this.labelColor = ColorProcessor.fromSettings(this.settings.labelColor);

    this.showIcon = this.settings.showIcon;
    this.icon = this.settings.icon;
    this.iconStyle = iconStyle(this.settings.iconSize, this.settings.iconSizeUnit);
    this.iconColor = ColorProcessor.fromSettings(this.settings.iconColor);

    this.showIconBackground = this.settings.showIconBackground;
    if (this.showIconBackground) {
      this.iconBackgroundSize = this.settings.iconBackgroundSize + this.settings.iconBackgroundSizeUnit;
    } else {
      this.iconBackgroundSize = this.settings.iconSize + this.settings.iconSizeUnit;
    }
    this.iconBackgroundStyle = {
      width: this.iconBackgroundSize,
      height: this.iconBackgroundSize,
      borderRadius: '4px'
    };
    this.iconBackgroundColor = ColorProcessor.fromSettings(this.settings.iconBackgroundColor);

    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showChevron = this.settings.showChevron;
    this.chevronStyle = iconStyle(this.settings.chevronSize, this.settings.chevronSizeUnit);
    this.chevronStyle.color = this.settings.chevronColor;

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;
    this.hasTitle = this.ctx.widgetConfig.showTitle;
  }

  public ngAfterViewInit() {
    if (this.settings.autoScale) {
      const height = this.hasTitle ? layoutHeightWithTitle : layoutHeight;
      this.renderer.setStyle(this.countPanelContent.nativeElement, 'height', height + 'px');
      this.renderer.setStyle(this.countPanelContent.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.countPanelContent.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.panelResize$.observe(this.countPanel.nativeElement);
      this.onResize();
    }
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
  }

  public onInit() {
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    let value: any;
    if (tsValue) {
      value = tsValue[1];
      this.valueText = formatValue(value, 0);
    } else {
      this.valueText = 'N/A';
    }
    this.labelColor.update(value);
    this.iconColor.update(value);
    this.iconBackgroundColor.update(value);
    this.valueColor.update(value);
    this.cd.detectChanges();
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }

  private onResize() {
    const panelWidth = this.countPanel.nativeElement.getBoundingClientRect().width - layoutPadding;
    const panelHeight = this.countPanel.nativeElement.getBoundingClientRect().height - layoutPadding;
    const targetWidth = panelWidth;
    let minAspect = 0.25;
    if (this.settings.showChevron) {
      minAspect -= 0.05;
    }
    if (this.hasTitle) {
      minAspect += 0.15;
    }
    const aspect = Math.min(panelHeight / targetWidth, minAspect);
    const targetHeight = targetWidth * aspect;
    const height = this.hasTitle ? layoutHeightWithTitle : layoutHeight;
    const scale = targetHeight / height;
    const width = targetWidth / scale;
    this.renderer.setStyle(this.countPanelContent.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.countPanelContent.nativeElement, 'transform', `scale(${scale})`);
  }
}

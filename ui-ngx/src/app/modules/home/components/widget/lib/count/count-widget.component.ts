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

import { ChangeDetectorRef, Component, Input, OnInit, TemplateRef } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { formatValue } from '@core/utils';
import {
  ColorProcessor,
  ComponentStyle,
  getSingleTsValue,
  iconStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import {
  CountCardLayout,
  countDefaultSettings,
  CountWidgetSettings
} from '@home/components/widget/lib/count/count-widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-count-widget',
  templateUrl: './count-widget.component.html',
  styleUrls: ['./count-widget.component.scss']
})
export class CountWidgetComponent implements OnInit {

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

  constructor(private widgetComponent: WidgetComponent,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.countWidget = this;
    this.settings = {...countDefaultSettings(this.alarmElseEntity), ...this.ctx.settings};

    this.layout = this.settings.layout;

    this.showLabel = this.settings.showLabel;
    this.label = this.settings.label;
    this.labelStyle = textStyle(this.settings.labelFont, '0.4px');
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

    this.valueStyle = textStyle(this.settings.valueFont, '0.1px');
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showChevron = this.settings.showChevron;
    this.chevronStyle = iconStyle(this.settings.chevronSize, this.settings.chevronSizeUnit);
    this.chevronStyle.color = this.settings.chevronColor;
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
    const descriptors = this.ctx.actionsApi.getActionDescriptors('cardClick');
    if (descriptors.length) {
      $event.stopPropagation();
      const descriptor = descriptors[0];
      this.ctx.actionsApi.handleWidgetAction($event, descriptor);
    }
  }
}

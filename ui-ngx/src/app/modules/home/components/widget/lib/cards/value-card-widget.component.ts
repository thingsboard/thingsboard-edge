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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { DatePipe } from '@angular/common';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  getDataKey,
  getLabel,
  getSingleTsValue,
  iconStyle,
  overlayStyle,
  textStyle
} from '@home/components/widget/config/widget-settings.models';
import { valueCardDefaultSettings, ValueCardLayout, ValueCardWidgetSettings } from './value-card-widget.models';
import { WidgetComponent } from '@home/components/widget/widget.component';

@Component({
  selector: 'tb-value-card-widget',
  templateUrl: './value-card-widget.component.html',
  styleUrls: ['./value-card-widget.component.scss']
})
export class ValueCardWidgetComponent implements OnInit {

  settings: ValueCardWidgetSettings;

  valueCardLayout = ValueCardLayout;

  @Input()
  ctx: WidgetContext;

  layout: ValueCardLayout;
  showIcon = true;
  icon = '';
  iconStyle: ComponentStyle = {};
  iconColor: ColorProcessor;

  showLabel = true;
  label = '';
  labelStyle: ComponentStyle = {};
  labelColor: ColorProcessor;

  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  showDate = true;
  dateText = '';
  dateStyle: ComponentStyle = {};
  dateColor: ColorProcessor;

  backgroundStyle: ComponentStyle = {};
  overlayStyle: ComponentStyle = {};

  private horizontal = false;
  private dateFormat: string;
  private decimals = 0;
  private units = '';

  constructor(private date: DatePipe,
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
    this.label = getLabel(this.ctx.datasources);
    this.labelStyle = textStyle(this.settings.labelFont, '1.5', '0.25px');
    this.labelColor =  ColorProcessor.fromSettings(this.settings.labelColor);
    this.valueStyle = textStyle(this.settings.valueFont,  '100%', '0.13px');
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.showDate = this.settings.showDate;
    this.dateFormat = this.settings.dateFormat;
    this.dateStyle = textStyle(this.settings.dateFont,  '1.33', '0.25px');
    this.dateColor = ColorProcessor.fromSettings(this.settings.dateColor);

    this.backgroundStyle = backgroundStyle(this.settings.background);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    let value;
    if (tsValue) {
      value = tsValue[1];
      this.valueText = formatValue(value, this.decimals, this.units, true);
      this.dateText = this.date.transform(tsValue[0], this.dateFormat);
    } else {
      this.valueText = 'N/A';
      this.dateText = '';
    }
    this.iconColor.update(value);
    this.labelColor.update(value);
    this.valueColor.update(value);
    this.dateColor.update(value);
    this.cd.detectChanges();
  }
}

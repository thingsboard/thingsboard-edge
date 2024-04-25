///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { DataKey, Datasource, LegendPosition } from '@shared/models/widget.models';
import { BackgroundSettings, Font } from '@shared/models/widget-settings.models';
import { Renderer2 } from '@angular/core';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { EChartsAnimationSettings } from '@home/components/widget/lib/chart/echarts-widget.models';

export interface LatestChartDataItem {
  id: number;
  datasource: Datasource;
  dataKey: DataKey;
  value: number;
  hasValue: boolean;
  enabled: boolean;
}

export interface LatestChartLegendItem {
  dataKey?: DataKey;
  color: string;
  label: string;
  value: string;
  hasValue: boolean;
  total?: boolean;
}

export enum LatestChartTooltipValueType {
  absolute = 'absolute',
  percentage = 'percentage'
}

export const latestChartTooltipValueTypes = Object.keys(LatestChartTooltipValueType) as LatestChartTooltipValueType[];

export const latestChartTooltipValueTypeTranslations = new Map<LatestChartTooltipValueType, string>(
  [
    [LatestChartTooltipValueType.absolute, 'widgets.latest-chart.tooltip-value-type-absolute'],
    [LatestChartTooltipValueType.percentage, 'widgets.latest-chart.tooltip-value-type-percentage']
  ]
);

export interface LatestChartTooltipSettings {
  showTooltip: boolean;
  tooltipValueType: LatestChartTooltipValueType;
  tooltipValueDecimals: number;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
}

export interface LatestChartSettings extends LatestChartTooltipSettings {
  autoScale?: boolean;
  sortSeries: boolean;
  showTotal?: boolean;
  showLegend: boolean;
  animation: EChartsAnimationSettings;
}

export interface LatestChartWidgetSettings extends LatestChartSettings {
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  background: BackgroundSettings;
}

export const latestChartTooltipFormatter = (renderer: Renderer2,
                                            settings: LatestChartTooltipSettings,
                                            params: CallbackDataParams,
                                            units: string,
                                            total: number): null | HTMLElement => {
  if (!params.name) {
    return null;
  }
  let value: string;
  if (settings.tooltipValueType === LatestChartTooltipValueType.percentage) {
    const percents = isDefinedAndNotNull(params.percent) ? params.percent : (params.value as number) / total * 100;
    value = formatValue(percents, settings.tooltipValueDecimals, '%', false);
  } else {
    value = formatValue(params.value, settings.tooltipValueDecimals, units, false);
  }
  const textElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(textElement, 'display', 'inline-flex');
  renderer.setStyle(textElement, 'align-items', 'center');
  renderer.setStyle(textElement, 'gap', '8px');
  const labelElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(labelElement, renderer.createText(params.name));
  renderer.setStyle(labelElement, 'font-family', 'Roboto');
  renderer.setStyle(labelElement, 'font-size', '11px');
  renderer.setStyle(labelElement, 'font-style', 'normal');
  renderer.setStyle(labelElement, 'font-weight', '400');
  renderer.setStyle(labelElement, 'line-height', '16px');
  renderer.setStyle(labelElement, 'letter-spacing', '0.25px');
  renderer.setStyle(labelElement, 'color', 'rgba(0, 0, 0, 0.38)');
  const valueElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(valueElement, renderer.createText(value));
  renderer.setStyle(valueElement, 'font-family', settings.tooltipValueFont.family);
  renderer.setStyle(valueElement, 'font-size', settings.tooltipValueFont.size + settings.tooltipValueFont.sizeUnit);
  renderer.setStyle(valueElement, 'font-style', settings.tooltipValueFont.style);
  renderer.setStyle(valueElement, 'font-weight', settings.tooltipValueFont.weight);
  renderer.setStyle(valueElement, 'line-height', settings.tooltipValueFont.lineHeight);
  renderer.setStyle(valueElement, 'color', settings.tooltipValueColor);
  renderer.appendChild(textElement, labelElement);
  renderer.appendChild(textElement, valueElement);
  return textElement;
};

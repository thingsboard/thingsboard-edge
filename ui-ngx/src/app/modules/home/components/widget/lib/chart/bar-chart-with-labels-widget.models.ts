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
  BackgroundSettings,
  BackgroundType,
  ComponentStyle,
  customDateFormat,
  Font,
  textStyle
} from '@shared/models/widget-settings.models';
import { LegendPosition } from '@shared/models/widget.models';
import { DeepPartial } from '@shared/models/common';
import {
  defaultTimeSeriesChartXAxisSettings,
  defaultTimeSeriesChartYAxisSettings,
  timeSeriesChartGridDefaultSettings,
  TimeSeriesChartGridSettings,
  TimeSeriesChartKeySettings,
  timeSeriesChartNoAggregationBarWidthDefaultSettings,
  TimeSeriesChartNoAggregationBarWidthSettings,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings,
  TimeSeriesChartThreshold,
  TimeSeriesChartXAxisSettings,
  TimeSeriesChartYAxisSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { CallbackDataParams, LabelLayoutOptionCallbackParams } from 'echarts/types/dist/shared';
import { formatValue, mergeDeep } from '@core/utils';
import { LabelLayoutOption } from 'echarts/types/src/util/types';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  ChartFillSettings,
  ChartFillType
} from '@home/components/widget/lib/chart/chart.models';
import {
  TimeSeriesChartTooltipWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';

export interface BarChartWithLabelsWidgetSettings extends TimeSeriesChartTooltipWidgetSettings {
  dataZoom: boolean;
  showBarLabel: boolean;
  barLabelFont: Font;
  barLabelColor: string;
  showBarValue: boolean;
  barValueFont: Font;
  barValueColor: string;
  showBarBorder: boolean;
  barBorderWidth: number;
  barBorderRadius: number;
  barBackgroundSettings: ChartFillSettings;
  noAggregationBarWidthSettings: TimeSeriesChartNoAggregationBarWidthSettings;
  grid: TimeSeriesChartGridSettings;
  yAxis: TimeSeriesChartYAxisSettings;
  xAxis: TimeSeriesChartXAxisSettings;
  animation: ChartAnimationSettings;
  thresholds: TimeSeriesChartThreshold[];
  showLegend: boolean;
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const barChartWithLabelsDefaultSettings: BarChartWithLabelsWidgetSettings = {
  dataZoom: false,
  showBarLabel: true,
  barLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '12px'
  },
  barLabelColor: 'rgba(0, 0, 0, 0.54)',
  showBarValue: true,
  barValueFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '700',
    lineHeight: '12px'
  },
  barValueColor: 'rgba(0, 0, 0, 0.76)',
  showBarBorder: false,
  barBorderWidth: 2,
  barBorderRadius: 0,
  barBackgroundSettings: {
    type: ChartFillType.none,
    opacity: 0.4,
    gradient: {
      start: 100,
      end: 0
    }
  },
  noAggregationBarWidthSettings: mergeDeep({} as TimeSeriesChartNoAggregationBarWidthSettings,
    timeSeriesChartNoAggregationBarWidthDefaultSettings),
  grid: mergeDeep({} as TimeSeriesChartGridSettings,
    timeSeriesChartGridDefaultSettings),
  yAxis: mergeDeep({} as TimeSeriesChartYAxisSettings,
    defaultTimeSeriesChartYAxisSettings,
    { id: 'default', order: 0, showLine: false, showTicks: false } as TimeSeriesChartYAxisSettings),
  xAxis: mergeDeep({} as TimeSeriesChartXAxisSettings,
    defaultTimeSeriesChartXAxisSettings,
    {showTicks: false, showSplitLines: false} as TimeSeriesChartXAxisSettings),
  animation: mergeDeep({} as ChartAnimationSettings,
    chartAnimationDefaultSettings),
  thresholds: [],
  showLegend: true,
  legendPosition: LegendPosition.top,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.76)',
  showTooltip: true,
  tooltipLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  tooltipLabelColor: 'rgba(0, 0, 0, 0.76)',
  tooltipValueFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
  tooltipShowDate: true,
  tooltipDateInterval: true,
  tooltipDateFormat: customDateFormat('MMMM y'),
  tooltipDateFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0, 0, 0, 0.76)',
  tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
  tooltipBackgroundBlur: 4,
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};

export const barChartWithLabelsTimeSeriesSettings = (settings: BarChartWithLabelsWidgetSettings): DeepPartial<TimeSeriesChartSettings> => ({
  dataZoom: settings.dataZoom,
  grid: settings.grid,
  yAxes: {
    default: settings.yAxis
  },
  xAxis: settings.xAxis,
  barWidthSettings: {
    barGap: 0,
    intervalGap: 0.5
  },
  noAggregationBarWidthSettings: settings.noAggregationBarWidthSettings,
  animation: settings.animation,
  thresholds: settings.thresholds,
  showTooltip: settings.showTooltip,
  tooltipLabelFont: settings.tooltipLabelFont,
  tooltipLabelColor: settings.tooltipLabelColor,
  tooltipValueFont: settings.tooltipValueFont,
  tooltipValueColor: settings.tooltipValueColor,
  tooltipShowDate: settings.tooltipShowDate,
  tooltipDateInterval: settings.tooltipDateInterval,
  tooltipDateFormat: settings.tooltipDateFormat,
  tooltipDateFont: settings.tooltipDateFont,
  tooltipDateColor: settings.tooltipDateColor,
  tooltipBackgroundColor: settings.tooltipBackgroundColor,
  tooltipBackgroundBlur: settings.tooltipBackgroundBlur,
  tooltipShowFocusedSeries: true
});

export const barChartWithLabelsTimeSeriesKeySettings = (settings: BarChartWithLabelsWidgetSettings,
                                                        decimals: number): DeepPartial<TimeSeriesChartKeySettings> => {
  const barValueStyle: ComponentStyle = textStyle(settings.barValueFont);
  delete barValueStyle.lineHeight;
  barValueStyle.fontSize = settings.barValueFont.size;
  barValueStyle.fill = settings.barValueColor;

  const barLabelStyle: ComponentStyle = textStyle(settings.barLabelFont);
  delete barLabelStyle.lineHeight;
  barLabelStyle.fontSize = settings.barLabelFont.size;
  barLabelStyle.fill = settings.barLabelColor;
  return {
    type: TimeSeriesChartSeriesType.bar,
    barSettings: {
      showBorder: settings.showBarBorder,
      borderWidth: settings.barBorderWidth,
      borderRadius: settings.barBorderRadius,
      backgroundSettings: settings.barBackgroundSettings,
      showLabel: settings.showBarLabel || settings.showBarValue,
      labelPosition: 'insideBottom',
      labelFormatter: (params: CallbackDataParams): string => {
        const labelParts: string[] = [];
        if (settings.showBarValue) {
          const labelValue = formatValue(params.value[1], decimals, '', false);
          labelParts.push(`{value|${labelValue}}`);
        }
        if (settings.showBarLabel) {
          labelParts.push(`{label|${params.seriesName}}`);
        }
        return labelParts.join(' ');
      },
      labelLayout: (params: LabelLayoutOptionCallbackParams): LabelLayoutOption => {
        if (params.rect.width - params.labelRect.width < 2) {
          return {
            y: '100000%',
          };
        } else {
          return {
            hideOverlap: true
          };
        }
      },
      additionalLabelOption: {
        textRotation: Math.PI / 2,
        textDistance: 15,
        textStrokeWidth: 0,
        textAlign: 'left',
        textVerticalAlign: 'middle',
        rich: {
          value: barValueStyle,
          label: barLabelStyle
        }
      }
    }
  };
};

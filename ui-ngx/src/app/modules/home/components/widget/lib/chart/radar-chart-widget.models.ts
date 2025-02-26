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
  latestChartWidgetDefaultSettings,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  ChartAnimationSettings,
  chartColorScheme,
  ChartFillSettings,
  ChartFillType,
  ChartLabelPosition,
  ChartLineType,
  ChartShape
} from '@home/components/widget/lib/chart/chart.models';
import { Font } from '@shared/models/widget-settings.models';
import { mergeDeep } from '@core/utils';
import {
  radarChartAnimationDefaultSettings,
  RadarChartSettings,
  RadarChartShape
} from '@home/components/widget/lib/chart/radar-chart.models';
import { DeepPartial } from '@shared/models/common';

export interface RadarChartWidgetSettings extends LatestChartWidgetSettings {
  shape: RadarChartShape;
  color: string;
  showLine: boolean;
  lineType: ChartLineType;
  lineWidth: number;
  showPoints: boolean;
  pointShape: ChartShape;
  pointSize: number;
  showLabel: boolean;
  labelPosition: ChartLabelPosition;
  labelFont: Font;
  labelColor: string;
  fillAreaSettings: ChartFillSettings;
  axisShowLabel: boolean;
  axisLabelFont: Font;
  axisShowTickLabels: boolean;
  axisTickLabelFont: Font;
  axisTickLabelColor: string;
}

export const radarChartWidgetDefaultSettings: RadarChartWidgetSettings = {
  ...latestChartWidgetDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    radarChartAnimationDefaultSettings),
  shape: RadarChartShape.polygon,
  color: 'var(--tb-primary-500)',
  showLine: true,
  lineType: ChartLineType.solid,
  lineWidth: 2,
  showPoints: true,
  pointShape: ChartShape.circle,
  pointSize: 4,
  showLabel: false,
  labelPosition: ChartLabelPosition.top,
  labelFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  labelColor: chartColorScheme['series.label'].light,
  fillAreaSettings: {
    type: ChartFillType.none,
    opacity: 0.4,
    gradient: {
      start: 80,
      end: 20
    }
  },
  axisShowLabel: true,
  axisLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '600',
    lineHeight: '1'
  },
  axisShowTickLabels: false,
  axisTickLabelFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  axisTickLabelColor: chartColorScheme['axis.tickLabel'].light
};

export const radarChartWidgetRadarChartSettings = (settings: RadarChartWidgetSettings): DeepPartial<RadarChartSettings> => ({
  shape: settings.shape,
  color: settings.color,
  showLine: settings.showLine,
  lineType: settings.lineType,
  lineWidth: settings.lineWidth,
  showPoints: settings.showPoints,
  pointShape: settings.pointShape,
  pointSize: settings.pointSize,
  showLabel: settings.showLabel,
  labelPosition: settings.labelPosition,
  labelFont: settings.labelFont,
  labelColor: settings.labelColor,
  fillAreaSettings: settings.fillAreaSettings,
  axisShowLabel: settings.axisShowLabel,
  axisLabelFont: settings.axisLabelFont,
  axisShowTickLabels: settings.axisShowTickLabels,
  axisTickLabelFont: settings.axisTickLabelFont,
  axisTickLabelColor: settings.axisTickLabelColor,
  sortSeries: settings.sortSeries,
  showTotal: false,
  animation: settings.animation,
  showLegend: settings.showLegend,
  showTooltip: settings.showTooltip,
  tooltipValueType: settings.tooltipValueType,
  tooltipValueDecimals: settings.tooltipValueDecimals,
  tooltipValueFont: settings.tooltipValueFont,
  tooltipValueColor: settings.tooltipValueColor,
  tooltipBackgroundColor: settings.tooltipBackgroundColor,
  tooltipBackgroundBlur: settings.tooltipBackgroundBlur
});

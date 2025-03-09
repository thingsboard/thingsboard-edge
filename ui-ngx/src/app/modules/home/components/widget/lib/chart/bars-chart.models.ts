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

import { latestChartDefaultSettings, LatestChartSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import { mergeDeep } from '@core/utils';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  chartBarDefaultSettings,
  ChartBarSettings,
  chartColorScheme
} from '@home/components/widget/lib/chart/chart.models';
import { Font } from '@shared/models/widget-settings.models';

export interface BarsChartSettings extends LatestChartSettings {
  polar: boolean;
  axisMin?: number | string;
  axisMax?: number | string;
  axisTickLabelFont: Font;
  axisTickLabelColor: string;
  angleAxisStartAngle?: number;
  barSettings: ChartBarSettings;
}

export const barsChartAnimationDefaultSettings: ChartAnimationSettings =
  mergeDeep({} as ChartAnimationSettings, chartAnimationDefaultSettings, {
    animationDuration: 1000,
    animationDurationUpdate: 500
  } as ChartAnimationSettings);

export const barsChartDefaultSettings: BarsChartSettings = {
  ...latestChartDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    barsChartAnimationDefaultSettings),
  polar: false,
  axisTickLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  axisTickLabelColor: chartColorScheme['axis.tickLabel'].light,
  angleAxisStartAngle: 0,
  barSettings: mergeDeep({} as ChartBarSettings, chartBarDefaultSettings,
    {barWidth: 80, showLabel: true} as ChartBarSettings)
};

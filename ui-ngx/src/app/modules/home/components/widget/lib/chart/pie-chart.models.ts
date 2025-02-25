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

import { ColorSettings, constantColor, Font } from '@shared/models/widget-settings.models';
import { latestChartDefaultSettings, LatestChartSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import { mergeDeep } from '@core/utils';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  PieChartLabelPosition
} from '@home/components/widget/lib/chart/chart.models';

export interface PieChartSettings extends LatestChartSettings {
  doughnut: boolean;
  radius: string;
  clockwise: boolean;
  totalValueFont: Font;
  totalValueColor: ColorSettings;
  showLabel: boolean;
  labelPosition: PieChartLabelPosition;
  labelFont: Font;
  labelColor: string;
  borderWidth: number;
  borderColor: string;
  borderRadius: string;
  emphasisScale: boolean;
  emphasisBorderWidth: number;
  emphasisBorderColor: string;
  emphasisShadowBlur: number;
  emphasisShadowColor: string;
}

export const pieChartAnimationDefaultSettings: ChartAnimationSettings =
  mergeDeep({} as ChartAnimationSettings, chartAnimationDefaultSettings, {
    animationDuration: 1000,
    animationDurationUpdate: 500
  } as ChartAnimationSettings);

export const pieChartDefaultSettings: PieChartSettings = {
  ...latestChartDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    pieChartAnimationDefaultSettings),
  doughnut: false,
  radius: '80%',
  clockwise: false,
  totalValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1'
  },
  totalValueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showLabel: false,
  labelPosition: PieChartLabelPosition.outside,
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: 'normal',
    lineHeight: '1'
  },
  labelColor: '#000',
  borderWidth: 0,
  borderColor: '#000',
  borderRadius: '0%',
  emphasisScale: true,
  emphasisBorderWidth: 0,
  emphasisBorderColor: '#000',
  emphasisShadowBlur: 10,
  emphasisShadowColor: 'rgba(0, 0, 0, 0.5)'
};

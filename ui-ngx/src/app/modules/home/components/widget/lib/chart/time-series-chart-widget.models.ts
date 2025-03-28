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
  timeSeriesChartDefaultSettings,
  TimeSeriesChartSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { BackgroundSettings, BackgroundType, Font } from '@shared/models/widget-settings.models';
import { defaultLegendConfig, LegendConfig, LegendPosition, widgetType } from '@shared/models/widget.models';
import { mergeDeep } from '@core/utils';

export interface TimeSeriesChartWidgetSettings extends TimeSeriesChartSettings {
  showLegend: boolean;
  legendColumnTitleFont: Font;
  legendColumnTitleColor: string;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  legendConfig: LegendConfig;
  background: BackgroundSettings;
  padding: string;
}

export const timeSeriesChartWidgetDefaultSettings: TimeSeriesChartWidgetSettings =
  mergeDeep({} as TimeSeriesChartWidgetSettings, timeSeriesChartDefaultSettings as TimeSeriesChartWidgetSettings, {
    showLegend: true,
    legendColumnTitleFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '16px'
    },
    legendColumnTitleColor: 'rgba(0, 0, 0, 0.38)',
    legendLabelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '16px'
    },
    legendLabelColor: 'rgba(0, 0, 0, 0.76)',
    legendValueFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500',
      lineHeight: '16px'
    },
    legendValueColor: 'rgba(0, 0, 0, 0.87)',
    legendConfig: {...defaultLegendConfig(widgetType.timeseries), position: LegendPosition.top},
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
  } as TimeSeriesChartWidgetSettings);

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
import { LegendPosition } from '@shared/models/widget.models';
import { pieChartAnimationDefaultSettings, PieChartSettings } from '@home/components/widget/lib/chart/pie-chart.models';
import { DeepPartial } from '@shared/models/common';
import {
  latestChartWidgetDefaultSettings,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { mergeDeep } from '@core/utils';
import { ChartAnimationSettings } from '@home/components/widget/lib/chart/chart.models';

export enum DoughnutLayout {
  default = 'default',
  with_total = 'with_total'
}

export const doughnutLayouts = Object.keys(DoughnutLayout) as DoughnutLayout[];

export const doughnutLayoutTranslations = new Map<DoughnutLayout, string>(
  [
    [DoughnutLayout.default, 'widgets.doughnut.layout-default'],
    [DoughnutLayout.with_total, 'widgets.doughnut.layout-with-total']
  ]
);

export const doughnutLayoutImages = new Map<DoughnutLayout, string>(
  [
    [DoughnutLayout.default, 'assets/widget/doughnut/default-layout.svg'],
    [DoughnutLayout.with_total, 'assets/widget/doughnut/with-total-layout.svg']
  ]
);

export const horizontalDoughnutLayoutImages = new Map<DoughnutLayout, string>(
  [
    [DoughnutLayout.default, 'assets/widget/doughnut/horizontal-default-layout.svg'],
    [DoughnutLayout.with_total, 'assets/widget/doughnut/horizontal-with-total-layout.svg']
  ]
);

export interface DoughnutWidgetSettings extends LatestChartWidgetSettings {
  layout: DoughnutLayout;
  clockwise: boolean;
  totalValueFont: Font;
  totalValueColor: ColorSettings;
}

export const doughnutDefaultSettings = (horizontal: boolean): DoughnutWidgetSettings => ({
  ...latestChartWidgetDefaultSettings,
  autoScale: true,
  sortSeries: false,
  animation: mergeDeep({} as ChartAnimationSettings,
    pieChartAnimationDefaultSettings),
  legendPosition: horizontal ? LegendPosition.right : LegendPosition.bottom,
  layout: DoughnutLayout.default,
  clockwise: false,
  totalValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1'
  },
  totalValueColor: constantColor('rgba(0, 0, 0, 0.87)')
});

export const doughnutPieChartSettings = (settings: DoughnutWidgetSettings): DeepPartial<PieChartSettings> => ({
  autoScale: settings.autoScale,
  doughnut: true,
  clockwise: settings.clockwise,
  sortSeries: settings.sortSeries,
  showTotal: settings.layout === DoughnutLayout.with_total,
  animation: settings.animation,
  showLegend: settings.showLegend,
  totalValueFont: settings.totalValueFont,
  totalValueColor: settings.totalValueColor,
  showLabel: false,
  borderWidth: 0,
  borderColor: '#fff',
  borderRadius: '50%',
  emphasisScale: false,
  emphasisBorderWidth: 2,
  emphasisBorderColor: '#fff',
  emphasisShadowColor: 'rgba(0, 0, 0, 0.24)',
  emphasisShadowBlur: 8,
  showTooltip: settings.showTooltip,
  tooltipValueType: settings.tooltipValueType,
  tooltipValueDecimals: settings.tooltipValueDecimals,
  tooltipValueFont: settings.tooltipValueFont,
  tooltipValueColor: settings.tooltipValueColor,
  tooltipBackgroundColor: settings.tooltipBackgroundColor,
  tooltipBackgroundBlur: settings.tooltipBackgroundBlur
});

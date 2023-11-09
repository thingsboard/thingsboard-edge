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
  BackgroundSettings,
  BackgroundType,
  ColorSettings,
  constantColor,
  Font
} from '@shared/models/widget-settings.models';

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

export enum DoughnutLegendPosition {
  top = 'top',
  bottom = 'bottom',
  left = 'left',
  right = 'right'
}

export const doughnutLegendPositionTranslations = new Map<DoughnutLegendPosition, string>(
  [
    [DoughnutLegendPosition.top, 'widgets.doughnut.legend-position-top'],
    [DoughnutLegendPosition.bottom, 'widgets.doughnut.legend-position-bottom'],
    [DoughnutLegendPosition.left, 'widgets.doughnut.legend-position-left'],
    [DoughnutLegendPosition.right, 'widgets.doughnut.legend-position-right']
  ]
);

export enum DoughnutTooltipValueType {
  absolute = 'absolute',
  percentage = 'percentage'
}

export const doughnutTooltipValueTypes = Object.keys(DoughnutTooltipValueType) as DoughnutTooltipValueType[];

export const doughnutTooltipValueTypeTranslations = new Map<DoughnutTooltipValueType, string>(
  [
    [DoughnutTooltipValueType.absolute, 'widgets.doughnut.tooltip-value-type-absolute'],
    [DoughnutTooltipValueType.percentage, 'widgets.doughnut.tooltip-value-type-percentage']
  ]
);

export interface DoughnutWidgetSettings {
  layout: DoughnutLayout;
  autoScale: boolean;
  clockwise: boolean;
  sortSeries: boolean;
  totalValueFont: Font;
  totalValueColor: ColorSettings;
  showLegend: boolean;
  legendPosition: DoughnutLegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  showTooltip: boolean;
  tooltipValueType: DoughnutTooltipValueType;
  tooltipValueDecimals: number;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
  background: BackgroundSettings;
}

export const doughnutDefaultSettings = (horizontal: boolean): DoughnutWidgetSettings => ({
  layout: DoughnutLayout.default,
  autoScale: true,
  clockwise: false,
  sortSeries: false,
  totalValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1'
  },
  totalValueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showLegend: true,
  legendPosition: horizontal ? DoughnutLegendPosition.right : DoughnutLegendPosition.bottom,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.38)',
  legendValueFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
  },
  legendValueColor: 'rgba(0, 0, 0, 0.87)',
  showTooltip: true,
  tooltipValueType: DoughnutTooltipValueType.percentage,
  tooltipValueDecimals: 0,
  tooltipValueFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
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
  }
});

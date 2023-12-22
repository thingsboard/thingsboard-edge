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

export enum ValueChartCardLayout {
  left = 'left',
  right = 'right'
}

export const valueCartCardLayouts = Object.keys(ValueChartCardLayout) as ValueChartCardLayout[];

export const valueChartCardLayoutTranslations = new Map<ValueChartCardLayout, string>(
  [
    [ValueChartCardLayout.left, 'widgets.value-chart-card.layout-left'],
    [ValueChartCardLayout.right, 'widgets.value-chart-card.layout-right']
  ]
);

export const valueChartCardLayoutImages = new Map<ValueChartCardLayout, string>(
  [
    [ValueChartCardLayout.left, 'assets/widget/value-chart-card/left-layout.svg'],
    [ValueChartCardLayout.right, 'assets/widget/value-chart-card/right-layout.svg']
  ]
);

export interface ValueChartCardWidgetSettings {
  layout: ValueChartCardLayout;
  autoScale: boolean;
  showValue: boolean;
  valueFont: Font;
  valueColor: ColorSettings;
  background: BackgroundSettings;
}

export const valueChartCardDefaultSettings: ValueChartCardWidgetSettings = {
  layout: ValueChartCardLayout.left,
  autoScale: true,
  showValue: true,
  valueFont: {
    family: 'Roboto',
    size: 28,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '32px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  }
};

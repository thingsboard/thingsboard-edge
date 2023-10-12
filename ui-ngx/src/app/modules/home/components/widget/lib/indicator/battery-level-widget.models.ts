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
  ColorType,
  constantColor,
  defaultColorFunction,
  Font
} from '@shared/models/widget-settings.models';

export enum BatteryLevelLayout {
  vertical_solid = 'vertical_solid',
  horizontal_solid = 'horizontal_solid',
  vertical_divided = 'vertical_divided',
  horizontal_divided = 'horizontal_divided'
}

export const batteryLevelLayouts = Object.keys(BatteryLevelLayout) as BatteryLevelLayout[];

export const batteryLevelLayoutTranslations = new Map<BatteryLevelLayout, string>(
  [
    [BatteryLevelLayout.vertical_solid, 'widgets.battery-level.layout-vertical-solid'],
    [BatteryLevelLayout.horizontal_solid, 'widgets.battery-level.layout-horizontal-solid'],
    [BatteryLevelLayout.vertical_divided, 'widgets.battery-level.layout-vertical-divided'],
    [BatteryLevelLayout.horizontal_divided, 'widgets.battery-level.layout-horizontal-divided']
  ]
);

export const batteryLevelLayoutImages = new Map<BatteryLevelLayout, string>(
  [
    [BatteryLevelLayout.vertical_solid, 'assets/widget/battery-level/vertical-solid-layout.svg'],
    [BatteryLevelLayout.horizontal_solid, 'assets/widget/battery-level/horizontal-solid-layout.svg'],
    [BatteryLevelLayout.vertical_divided, 'assets/widget/battery-level/vertical-divided-layout.svg'],
    [BatteryLevelLayout.horizontal_divided, 'assets/widget/battery-level/horizontal-divided-layout.svg']
  ]
);

export interface BatteryLevelWidgetSettings {
  layout: BatteryLevelLayout;
  sectionsCount: number;
  showValue: boolean;
  autoScaleValueSize: boolean;
  valueFont: Font;
  valueColor: ColorSettings;
  batteryLevelColor: ColorSettings;
  batteryShapeColor: ColorSettings;
  background: BackgroundSettings;
}

export const batteryLevelDefaultSettings: BatteryLevelWidgetSettings = {
  layout: BatteryLevelLayout.vertical_solid,
  sectionsCount: 4,
  showValue: true,
  autoScaleValueSize: true,
  valueFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '24px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  batteryLevelColor: {
    color: 'rgba(92, 223, 144, 1)',
    type: ColorType.range,
    rangeList: [
      {from: 0, to: 25, color: 'rgba(227, 71, 71, 1)'},
      {from: 25, to: 50, color: 'rgba(246, 206, 67, 1)'},
      {from: 50, to: 100, color: 'rgba(92, 223, 144, 1)'}
    ],
    colorFunction: defaultColorFunction
  },
  batteryShapeColor: {
    color: 'rgba(92, 223, 144, 0.32)',
    type: ColorType.range,
    rangeList: [
      {from: 0, to: 25, color: 'rgba(227, 71, 71, 0.32)'},
      {from: 25, to: 50, color: 'rgba(246, 206, 67, 0.32)'},
      {from: 50, to: 100, color: 'rgba(92, 223, 144, 0.32)'}
    ],
    colorFunction: defaultColorFunction
  },
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


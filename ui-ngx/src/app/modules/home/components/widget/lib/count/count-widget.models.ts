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
  ColorSettings,
  ColorType,
  constantColor,
  cssUnit,
  defaultColorFunction,
  Font
} from '@shared/models/widget-settings.models';

export enum CountCardLayout {
  column = 'column',
  row = 'row'
}

export const countCardLayouts = Object.keys(CountCardLayout) as CountCardLayout[];

export const countCardLayoutTranslations = new Map<CountCardLayout, string>(
  [
    [CountCardLayout.column, 'widgets.count.layout-column'],
    [CountCardLayout.row, 'widgets.count.layout-row']
  ]
);

export const alarmCountCardLayoutImages = new Map<CountCardLayout, string>(
  [
    [CountCardLayout.column, 'assets/widget/alarm-count/column-layout.svg'],
    [CountCardLayout.row, 'assets/widget/alarm-count/row-layout.svg']
  ]
);

export const entityCountCardLayoutImages = new Map<CountCardLayout, string>(
  [
    [CountCardLayout.column, 'assets/widget/entity-count/column-layout.svg'],
    [CountCardLayout.row, 'assets/widget/entity-count/row-layout.svg']
  ]
);

export interface CountWidgetSettings {
  layout: CountCardLayout;
  autoScale: boolean;
  showLabel: boolean;
  label: string;
  labelFont: Font;
  labelColor: ColorSettings;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: ColorSettings;
  showIconBackground: boolean;
  iconBackgroundSize: number;
  iconBackgroundSizeUnit: cssUnit;
  iconBackgroundColor: ColorSettings;
  valueFont: Font;
  valueColor: ColorSettings;
  showChevron: boolean;
  chevronSize: number;
  chevronSizeUnit: cssUnit;
  chevronColor: string;
}

export const countDefaultSettings = (alarmElseEntity: boolean): CountWidgetSettings => ({
  layout: CountCardLayout.column,
  autoScale: true,
  showLabel: true,
  label: alarmElseEntity ? 'Total' : 'Devices',
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  labelColor: constantColor('rgba(0, 0, 0, 0.54)'),
  showIcon: true,
  icon: alarmElseEntity ? 'warning' : 'devices',
  iconSize: 20,
  iconSizeUnit: 'px',
  iconColor: constantColor('rgba(255, 255, 255, 1)'),
  showIconBackground: true,
  iconBackgroundSize: 36,
  iconBackgroundSizeUnit: 'px',
  iconBackgroundColor: alarmElseEntity
    ? {
      color: 'rgba(0, 105, 92, 1)',
      type: ColorType.range,
      rangeList: [
        {from: 0, to: 0, color: 'rgba(0, 105, 92, 1)'},
        {from: 1, color: 'rgba(209, 39, 48, 1)'}
      ],
      colorFunction: defaultColorFunction
    }
    : constantColor('rgba(241, 141, 23, 1)'),
  valueFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '24px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showChevron: false,
  chevronSize: 24,
  chevronSizeUnit: 'px',
  chevronColor: 'rgba(0, 0, 0, 0.38)'
});

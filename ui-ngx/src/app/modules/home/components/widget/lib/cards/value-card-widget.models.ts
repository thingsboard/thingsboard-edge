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
  cssUnit, DateFormatSettings,
  Font, lastUpdateAgoDateFormat
} from '@home/components/widget/config/widget-settings.models';

export enum ValueCardLayout {
  square = 'square',
  vertical = 'vertical',
  centered = 'centered',
  simplified = 'simplified',
  horizontal = 'horizontal',
  horizontal_reversed = 'horizontal_reversed'
}

export const valueCardLayouts = (horizontal: boolean): ValueCardLayout[] => {
  if (horizontal) {
    return [ValueCardLayout.horizontal, ValueCardLayout.horizontal_reversed];
  } else {
    return [ValueCardLayout.square, ValueCardLayout.vertical, ValueCardLayout.centered, ValueCardLayout.simplified];
  }
};

export const valueCardLayoutTranslations = new Map<ValueCardLayout, string>(
  [
    [ValueCardLayout.square, 'widgets.value-card.layout-square'],
    [ValueCardLayout.vertical, 'widgets.value-card.layout-vertical'],
    [ValueCardLayout.centered, 'widgets.value-card.layout-centered'],
    [ValueCardLayout.simplified, 'widgets.value-card.layout-simplified'],
    [ValueCardLayout.horizontal, 'widgets.value-card.layout-horizontal'],
    [ValueCardLayout.horizontal_reversed, 'widgets.value-card.layout-horizontal-reversed']
  ]
);

export const valueCardLayoutImages = new Map<ValueCardLayout, string>(
  [
    [ValueCardLayout.square, 'assets/widget/value-card/square-layout.svg'],
    [ValueCardLayout.vertical, 'assets/widget/value-card/vertical-layout.svg'],
    [ValueCardLayout.centered, 'assets/widget/value-card/centered-layout.svg'],
    [ValueCardLayout.simplified, 'assets/widget/value-card/simplified-layout.svg'],
    [ValueCardLayout.horizontal, 'assets/widget/value-card/horizontal-layout.svg'],
    [ValueCardLayout.horizontal_reversed, 'assets/widget/value-card/horizontal-reversed-layout.svg']
  ]
);

export interface ValueCardWidgetSettings {
  layout: ValueCardLayout;
  showLabel: boolean;
  labelFont: Font;
  labelColor: ColorSettings;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: ColorSettings;
  valueFont: Font;
  valueColor: ColorSettings;
  showDate: boolean;
  dateFormat: DateFormatSettings;
  dateFont: Font;
  dateColor: ColorSettings;
  background: BackgroundSettings;
}

export const valueCardDefaultSettings = (horizontal: boolean): ValueCardWidgetSettings => ({
  layout: horizontal ? ValueCardLayout.horizontal : ValueCardLayout.square,
  showLabel: true,
  labelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500'
  },
  labelColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showIcon: true,
  icon: 'thermostat',
  iconSize: 40,
  iconSizeUnit: 'px',
  iconColor: constantColor('#5469FF'),
  valueFont: {
    family: 'Roboto',
    size: 52,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showDate: true,
  dateFormat: lastUpdateAgoDateFormat(),
  dateFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500'
  },
  dateColor: constantColor('rgba(0, 0, 0, 0.38)'),
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

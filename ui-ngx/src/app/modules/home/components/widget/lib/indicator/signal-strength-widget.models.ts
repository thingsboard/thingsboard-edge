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
  DateFormatSettings,
  defaultColorFunction,
  Font,
  lastUpdateAgoDateFormat
} from '@shared/models/widget-settings.models';

export enum SignalStrengthLayout {
  wifi = 'wifi',
  cellular_bar = 'cellular_bar'
}

export const signalStrengthLayouts = Object.keys(SignalStrengthLayout) as SignalStrengthLayout[];

export const signalStrengthLayoutTranslations = new Map<SignalStrengthLayout, string>(
  [
    [SignalStrengthLayout.wifi, 'widgets.signal-strength.layout-wifi'],
    [SignalStrengthLayout.cellular_bar, 'widgets.signal-strength.layout-cellular-bar']
  ]
);

export const signalStrengthLayoutImages = new Map<SignalStrengthLayout, string>(
  [
    [SignalStrengthLayout.wifi, 'assets/widget/signal-strength/wifi-layout.svg'],
    [SignalStrengthLayout.cellular_bar, 'assets/widget/signal-strength/cellular-bar-layout.svg']
  ]
);

export interface SignalStrengthWidgetSettings {
  layout: SignalStrengthLayout;
  showDate: boolean;
  dateFormat: DateFormatSettings;
  dateFont: Font;
  dateColor: string;
  activeBarsColor: ColorSettings;
  inactiveBarsColor: string;
  showTooltip: boolean;
  showTooltipValue: boolean;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  showTooltipDate: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
  background: BackgroundSettings;
}

export const signalStrengthDefaultSettings: SignalStrengthWidgetSettings = {
  layout: SignalStrengthLayout.wifi,
  showDate: false,
  dateFormat: lastUpdateAgoDateFormat(),
  dateFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  dateColor: 'rgba(0, 0, 0, 0.38)',
  activeBarsColor: {
    color: 'rgba(92, 223, 144, 1)',
    type: ColorType.range,
    rangeList: [
      {to: -85, color: 'rgba(227, 71, 71, 1)'},
      {from: -85, to: -70, color: 'rgba(255, 122, 0, 1)'},
      {from: -70, to: -55, color: 'rgba(246, 206, 67, 1)'},
      {from: -55, color: 'rgba(92, 223, 144, 1)'}
    ],
    colorFunction: defaultColorFunction
  },
  inactiveBarsColor: 'rgba(224, 224, 224, 1)',
  showTooltip: true,
  showTooltipValue: true,
  tooltipValueFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0,0,0,0.76)',
  showTooltipDate: true,
  tooltipDateFormat: lastUpdateAgoDateFormat(),
  tooltipDateFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0,0,0,0.76)',
  tooltipBackgroundColor: 'rgba(255,255,255,0.72)',
  tooltipBackgroundBlur: 3,
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

export const signalBarActive = (rssi: number, index: number): boolean => {
    switch (index) {
      case 0:
        return rssi > -100;
      case 1:
        return rssi >= -85;
      case 2:
        return rssi >= -70;
      case 3:
        return rssi >= -55;
    }
};

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

import { BatteryLevelLayout } from '@home/components/widget/lib/indicator/battery-level-widget.models';
import {
  BackgroundSettings,
  BackgroundType,
  ColorSettings,
  constantColor,
  Font
} from '@shared/models/widget-settings.models';

export enum WindSpeedDirectionLayout {
  default = 'default',
  advanced = 'advanced',
  simplified = 'simplified'
}

export const windSpeedDirectionLayouts = Object.keys(WindSpeedDirectionLayout) as WindSpeedDirectionLayout[];

export const windSpeedDirectionLayoutTranslations = new Map<WindSpeedDirectionLayout, string>(
  [
    [WindSpeedDirectionLayout.default, 'widgets.wind-speed-direction.layout-default'],
    [WindSpeedDirectionLayout.advanced, 'widgets.wind-speed-direction.layout-advanced'],
    [WindSpeedDirectionLayout.simplified, 'widgets.wind-speed-direction.layout-simplified']
  ]
);

export const windSpeedDirectionLayoutImages = new Map<WindSpeedDirectionLayout, string>(
  [
    [WindSpeedDirectionLayout.default, 'assets/widget/wind-speed-direction/default-layout.svg'],
    [WindSpeedDirectionLayout.advanced, 'assets/widget/wind-speed-direction/advanced-layout.svg'],
    [WindSpeedDirectionLayout.simplified, 'assets/widget/wind-speed-direction/simplified-layout.svg']
  ]
);

export interface WindSpeedDirectionWidgetSettings {
  layout: WindSpeedDirectionLayout;
  centerValueFont: Font;
  centerValueColor: ColorSettings;
  ticksColor: string;
  arrowColor: string;
  directionalNamesElseDegrees: boolean;
  majorTicksColor: string;
  majorTicksFont: Font;
  minorTicksColor: string;
  minorTicksFont: Font;
  background: BackgroundSettings;
}

export const windSpeedDirectionDefaultSettings: WindSpeedDirectionWidgetSettings = {
  layout: WindSpeedDirectionLayout.default,
  centerValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '32px'
  },
  centerValueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  ticksColor: 'rgba(0, 0, 0, 0.12)',
  arrowColor: 'rgba(0, 0, 0, 0.87)',
  directionalNamesElseDegrees: true,
  majorTicksColor: 'rgba(158, 158, 158, 1)',
  majorTicksFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
  },
  minorTicksColor: 'rgba(0, 0, 0, 0.12)',
  minorTicksFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
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

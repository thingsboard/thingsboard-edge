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

export enum ProgressBarLayout {
  default = 'default',
  simplified = 'simplified'
}

export const progressBarLayouts = Object.keys(ProgressBarLayout) as ProgressBarLayout[];

export const progressBarLayoutTranslations = new Map<ProgressBarLayout, string>(
  [
    [ProgressBarLayout.default, 'widgets.progress-bar.layout-default'],
    [ProgressBarLayout.simplified, 'widgets.progress-bar.layout-simplified']
  ]
);

export const progressBarLayoutImages = new Map<ProgressBarLayout, string>(
  [
    [ProgressBarLayout.default, 'assets/widget/progress-bar/default-layout.svg'],
    [ProgressBarLayout.simplified, 'assets/widget/progress-bar/simplified-layout.svg']
  ]
);

export interface ProgressBarWidgetSettings {
  layout: ProgressBarLayout;
  autoScale: boolean;
  showValue: boolean;
  valueFont: Font;
  valueColor: ColorSettings;
  showTicks: boolean;
  tickMin: number;
  tickMax: number;
  ticksFont: Font;
  ticksColor: string;
  barColor: ColorSettings;
  barBackground: string;
  background: BackgroundSettings;
}

export const progressBarDefaultSettings: ProgressBarWidgetSettings = {
  layout: ProgressBarLayout.default,
  autoScale: true,
  showValue: true,
  valueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '32px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showTicks: true,
  tickMin: 0,
  tickMax: 100,
  ticksFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  ticksColor: 'rgba(0,0,0,0.54)',
  barColor: constantColor('rgba(63, 82, 221, 1)'),
  barBackground: 'rgba(0, 0, 0, 0.04)',
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

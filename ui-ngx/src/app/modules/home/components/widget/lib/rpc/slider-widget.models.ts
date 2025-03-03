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
  DataToValueType,
  GetValueAction,
  GetValueSettings,
  SetValueAction,
  SetValueSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';
import { BackgroundSettings, BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';

export enum SliderLayout {
  default = 'default',
  extended = 'extended',
  simplified = 'simplified'
}

export const sliderLayouts = Object.keys(SliderLayout) as SliderLayout[];

export const sliderLayoutTranslations = new Map<SliderLayout, string>(
  [
    [SliderLayout.default, 'widgets.slider.layout-default'],
    [SliderLayout.extended, 'widgets.slider.layout-extended'],
    [SliderLayout.simplified, 'widgets.slider.layout-simplified']
  ]
);

export const sliderLayoutImages = new Map<SliderLayout, string>(
  [
    [SliderLayout.default, 'assets/widget/slider/default-layout.svg'],
    [SliderLayout.extended, 'assets/widget/slider/extended-layout.svg'],
    [SliderLayout.simplified, 'assets/widget/slider/simplified-layout.svg']
  ]
);

export interface SliderWidgetSettings {
  initialState: GetValueSettings<number>;
  disabledState: GetValueSettings<boolean>;
  valueChange: SetValueSettings;
  layout: SliderLayout;
  autoScale: boolean;
  showValue: boolean;
  valueUnits: string;
  valueDecimals: number;
  valueFont: Font;
  valueColor: string;
  showTicks: boolean;
  tickMin: number;
  tickMax: number;
  ticksFont: Font;
  ticksColor: string;
  showTickMarks: boolean;
  tickMarksCount: number;
  tickMarksColor: string;
  mainColor: string;
  backgroundColor: string;
  mainColorDisabled: string;
  backgroundColorDisabled: string;
  leftIcon: string;
  leftIconSize: number;
  leftIconSizeUnit: cssUnit;
  leftIconColor: string;
  rightIcon: string;
  rightIconSize: number;
  rightIconSizeUnit: cssUnit;
  rightIconColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const sliderWidgetDefaultSettings: SliderWidgetSettings = {
  initialState: {
    action: GetValueAction.EXECUTE_RPC,
    defaultValue: 0,
    executeRpc: {
      method: 'getState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return integer value */\nreturn data;'
    }
  },
  disabledState: {
    action: GetValueAction.DO_NOTHING,
    defaultValue: false,
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  },
  valueChange: {
    action: SetValueAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    setAttribute: {
      key: 'state',
      scope: AttributeScope.SERVER_SCOPE
    },
    putTimeSeries: {
      key: 'state'
    },
    valueToData: {
      type: ValueToDataType.VALUE,
      constantValue: 0,
      valueToDataFunction: '/* Convert input integer value to RPC parameters or attribute/time-series value */\nreturn value;'
    }
  },
  layout: SliderLayout.default,
  autoScale: true,
  showValue: true,
  valueUnits: '%',
  valueDecimals: 0,
  valueFont: {
    family: 'Roboto',
    size: 36,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '36px'
  },
  valueColor: 'rgba(0, 0, 0, 0.87)',
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
  showTickMarks: true,
  tickMarksCount: 11,
  tickMarksColor: 'var(--tb-primary-500)',
  mainColor: 'var(--tb-primary-500)',
  backgroundColor: 'var(--tb-primary-100)',
  mainColorDisabled: '#9BA2B0',
  backgroundColorDisabled: '#D5D7E5',
  leftIcon: 'lightbulb',
  leftIconSize: 24,
  leftIconSizeUnit: 'px',
  leftIconColor: 'var(--tb-primary-500)',
  rightIcon: 'mdi:lightbulb-on',
  rightIconSize: 24,
  rightIconSizeUnit: 'px',
  rightIconColor: 'var(--tb-primary-500)',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '24px'
};

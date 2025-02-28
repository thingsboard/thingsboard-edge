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

import { BackgroundSettings, BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  DataToValueType,
  GetValueAction,
  GetValueSettings,
  SetValueAction,
  SetValueSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';

export enum SingleSwitchLayout {
  right = 'right',
  left = 'left',
  centered = 'centered'
}

export const singleSwitchLayouts = Object.keys(SingleSwitchLayout) as SingleSwitchLayout[];

export const singleSwitchLayoutTranslations = new Map<SingleSwitchLayout, string>(
  [
    [SingleSwitchLayout.right, 'widgets.single-switch.layout-right'],
    [SingleSwitchLayout.left, 'widgets.single-switch.layout-left'],
    [SingleSwitchLayout.centered, 'widgets.single-switch.layout-centered']
  ]
);

export const singleSwitchLayoutImages = new Map<SingleSwitchLayout, string>(
  [
    [SingleSwitchLayout.right, 'assets/widget/single-switch/right-layout.svg'],
    [SingleSwitchLayout.left, 'assets/widget/single-switch/left-layout.svg'],
    [SingleSwitchLayout.centered, 'assets/widget/single-switch/centered-layout.svg']
  ]
);

export interface SingleSwitchWidgetSettings {
  initialState: GetValueSettings<boolean>;
  disabledState: GetValueSettings<boolean>;
  onUpdateState: SetValueSettings;
  offUpdateState: SetValueSettings;
  layout: SingleSwitchLayout;
  autoScale: boolean;
  showLabel: boolean;
  label: string;
  labelFont: Font;
  labelColor: string;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: string;
  switchColorOn: string;
  switchColorOff: string;
  switchColorDisabled: string;
  tumblerColorOn: string;
  tumblerColorOff: string;
  tumblerColorDisabled: string;
  showOnLabel: boolean;
  onLabel: string;
  onLabelFont: Font;
  onLabelColor: string;
  showOffLabel: boolean;
  offLabel: string;
  offLabelFont: Font;
  offLabelColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const singleSwitchDefaultSettings: SingleSwitchWidgetSettings = {
  initialState: {
    action: GetValueAction.EXECUTE_RPC,
    defaultValue: false,
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
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
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
  onUpdateState: {
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
      type: ValueToDataType.CONSTANT,
      constantValue: true,
      valueToDataFunction: '/* Convert input boolean value to RPC parameters or attribute/time-series value */\nreturn value;'
    }
  },
  offUpdateState: {
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
      type: ValueToDataType.CONSTANT,
      constantValue: false,
      valueToDataFunction: '/* Convert input boolean value to RPC parameters or attribute/time-series value */ \n return value;'
    }
  },
  layout: SingleSwitchLayout.right,
  autoScale: true,
  showLabel: true,
  label: 'Switch',
  labelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '24px'
  },
  labelColor: 'rgba(0, 0, 0, 0.76)',
  showIcon: false,
  icon: 'mdi:lightbulb-outline',
  iconSize: 24,
  iconSizeUnit: 'px',
  iconColor: 'rgba(0, 0, 0, 0.76)',
  switchColorOn: 'var(--tb-primary-500)',
  switchColorOff: 'var(--tb-primary-100)',
  switchColorDisabled: '#D5D7E5',
  tumblerColorOn: '#fff',
  tumblerColorOff: '#fff',
  tumblerColorDisabled: '#fff',
  showOnLabel: false,
  onLabel: 'On',
  onLabelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
  },
  onLabelColor: 'rgba(0, 0, 0, 0.38)',
  showOffLabel: false,
  offLabel: 'Off',
  offLabelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
  },
  offLabelColor: 'rgba(0, 0, 0, 0.38)',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: ''
};

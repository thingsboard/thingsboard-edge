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

import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';
import { BackgroundSettings, BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';

export enum StatusWidgetLayout {
  default = 'default',
  center = 'center',
  icon = 'icon'
}

export const statusWidgetLayouts = Object.keys(StatusWidgetLayout) as StatusWidgetLayout[];

export const statusWidgetLayoutTranslations = new Map<StatusWidgetLayout, string>(
  [
    [StatusWidgetLayout.default, 'widgets.status-widget.layout-default'],
    [StatusWidgetLayout.center, 'widgets.status-widget.layout-center'],
    [StatusWidgetLayout.icon, 'widgets.status-widget.layout-icon']
  ]
);

export const statusWidgetLayoutImages = new Map<StatusWidgetLayout, string>(
  [
    [StatusWidgetLayout.default, 'assets/widget/status-widget/default-layout.svg'],
    [StatusWidgetLayout.center, 'assets/widget/status-widget/center-layout.svg'],
    [StatusWidgetLayout.icon, 'assets/widget/status-widget/icon-layout.svg']
  ]
);

export interface StatusWidgetStateSettings {
  showLabel: boolean;
  label: string;
  labelFont: Font;
  showStatus: boolean;
  status: string;
  statusFont: Font;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  primaryColor: string;
  secondaryColor: string;
  background: BackgroundSettings;
  primaryColorDisabled: string;
  secondaryColorDisabled: string;
  backgroundDisabled: BackgroundSettings;
}

export interface StatusWidgetSettings {
  initialState: GetValueSettings<boolean>;
  disabledState: GetValueSettings<boolean>;
  layout: StatusWidgetLayout;
  onState: StatusWidgetStateSettings;
  offState: StatusWidgetStateSettings;
  padding: string
}

export const statusWidgetDefaultSettings: StatusWidgetSettings = {
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
  layout: StatusWidgetLayout.default,
  onState: {
    showLabel: true,
    label: 'Window left corner',
    labelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500',
      lineHeight: '16px'
    },
    showStatus: true,
    status: 'Opened',
    statusFont: {
      family: 'Roboto',
      size: 10,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500',
      lineHeight: '20px'
    },
    icon: 'mdi:curtains',
    iconSize: 32,
    iconSizeUnit: 'px',
    primaryColor: '#fff',
    secondaryColor: 'rgba(255, 255, 255, 0.80)',
    background: {
      type: BackgroundType.color,
      color: 'var(--tb-primary-500)',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    },
    primaryColorDisabled: 'rgba(0, 0, 0, 0.38)',
    secondaryColorDisabled: 'rgba(0, 0, 0, 0.38)',
    backgroundDisabled: {
      type: BackgroundType.color,
      color: '#CACACA',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    }
  },
  offState: {
    showLabel: true,
    label: 'Window left corner',
    labelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500',
      lineHeight: '16px'
    },
    showStatus: true,
    status: 'Closed',
    statusFont: {
      family: 'Roboto',
      size: 10,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500',
      lineHeight: '20px'
    },
    icon: 'mdi:curtains-closed',
    iconSize: 32,
    iconSizeUnit: 'px',
    primaryColor: 'rgba(0, 0, 0, 0.87)',
    secondaryColor: 'rgba(0, 0, 0, 0.54)',
    background: {
      type: BackgroundType.color,
      color: '#FFF',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    },
    primaryColorDisabled: 'rgba(0, 0, 0, 0.38)',
    secondaryColorDisabled: 'rgba(0, 0, 0, 0.38)',
    backgroundDisabled: {
      type: BackgroundType.color,
      color: '#CACACA',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    }
  },
  padding: ''
};

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
  ColorProcessor,
  ColorSettings,
  ColorType,
  ComponentStyle,
  constantColor,
  DateFormatSettings,
  Font,
  lastUpdateAgoDateFormat,
  textStyle
} from '@shared/models/widget-settings.models';
import { ComparisonResultType, DataKey, DatasourceData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { AggregationType } from '@shared/models/time/time.models';

export interface AggregatedValueCardWidgetSettings {
  autoScale: boolean;
  showSubtitle: boolean;
  subtitle: string;
  subtitleFont: Font;
  subtitleColor: string;
  showDate: boolean;
  dateFormat: DateFormatSettings;
  dateFont: Font;
  dateColor: string;
  showChart: boolean;
  background: BackgroundSettings;
}

export enum AggregatedValueCardKeyPosition {
  center = 'center',
  rightTop = 'rightTop',
  rightBottom = 'rightBottom',
  leftTop = 'leftTop',
  leftBottom = 'leftBottom'
}

export const aggregatedValueCardKeyPositionTranslations = new Map<AggregatedValueCardKeyPosition, string>(
  [
    [AggregatedValueCardKeyPosition.center, 'widgets.aggregated-value-card.position-center'],
    [AggregatedValueCardKeyPosition.rightTop, 'widgets.aggregated-value-card.position-right-top'],
    [AggregatedValueCardKeyPosition.rightBottom, 'widgets.aggregated-value-card.position-right-bottom'],
    [AggregatedValueCardKeyPosition.leftTop, 'widgets.aggregated-value-card.position-left-top'],
    [AggregatedValueCardKeyPosition.leftBottom, 'widgets.aggregated-value-card.position-left-bottom']
  ]
);

export interface AggregatedValueCardKeySettings {
  position: AggregatedValueCardKeyPosition;
  font: Font;
  color: ColorSettings;
  showArrow: boolean;
}

export interface AggregatedValueCardValue {
  key: DataKey;
  value: string;
  units: string;
  style: ComponentStyle;
  color: ColorProcessor;
  center: boolean;
  showArrow: boolean;
  upArrow: boolean;
  downArrow: boolean;
}

export const computeAggregatedCardValue =
  (dataKeys: DataKey[], keyName: string, position: AggregatedValueCardKeyPosition): AggregatedValueCardValue => {
  const key = dataKeys.find(dataKey => ( dataKey.name === keyName && (dataKey.settings?.position === position ||
                                         (!dataKey.settings?.position && position === AggregatedValueCardKeyPosition.center)) ));
  if (key) {
    const settings: AggregatedValueCardKeySettings = key.settings;
    return {
      key,
      value: '',
      units: key.units,
      style: textStyle(settings.font, '0.25px'),
      color: ColorProcessor.fromSettings(settings.color),
      center: position === AggregatedValueCardKeyPosition.center,
      showArrow: settings.showArrow,
      upArrow: false,
      downArrow: false
    };
  }
};

export const getTsValueByLatestDataKey = (latestData: Array<DatasourceData>, dataKey: DataKey): [number, any] => {
  if (latestData?.length) {
    const dsData = latestData.find(data => data.dataKey === dataKey);
    if (dsData?.data?.length) {
      return dsData.data[0];
    }
  }
  return null;
};

export const aggregatedValueCardDefaultSettings: AggregatedValueCardWidgetSettings = {
  autoScale: true,
  showSubtitle: true,
  subtitle: '${entityName}',
  subtitleFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  subtitleColor: 'rgba(0, 0, 0, 0.38)',
  showDate: true,
  dateFormat: lastUpdateAgoDateFormat(),
  dateFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  dateColor: 'rgba(0, 0, 0, 0.38)',
  showChart: true,
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

export const aggregatedValueCardDefaultKeySettings: AggregatedValueCardKeySettings = {
  position: AggregatedValueCardKeyPosition.center,
  font: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1'
  },
  color: constantColor('rgba(0, 0, 0, 0.87)'),
  showArrow: false
};

export const createDefaultAggregatedValueLatestDataKeys = (keyName: string, units): DataKey[] => [
    {
      name: keyName, label: 'Latest', type: DataKeyType.timeseries, units, decimals: 0,
      aggregationType: AggregationType.NONE,
      settings: {
        position: AggregatedValueCardKeyPosition.center,
        font: {
          family: 'Roboto',
          size: 52,
          sizeUnit: 'px',
          style: 'normal',
          weight: '500',
          lineHeight: '1'
        },
        color: constantColor('rgba(0, 0, 0, 0.87)'),
        showArrow: false
      } as AggregatedValueCardKeySettings
    },
    {
      name: keyName, label: 'Delta percent', type: DataKeyType.timeseries, units: '%', decimals: 0,
      aggregationType: AggregationType.AVG,
      comparisonEnabled: true,
      timeForComparison: 'previousInterval',
      comparisonResultType: ComparisonResultType.DELTA_PERCENT,
      settings: {
        position: AggregatedValueCardKeyPosition.rightTop,
        font: {
          family: 'Roboto',
          size: 14,
          sizeUnit: 'px',
          style: 'normal',
          weight: '500',
          lineHeight: '1'
        },
        color: {
          color: 'rgba(0, 0, 0, 0.87)',
          type: ColorType.range,
          rangeList: [
            {to: 0, color: '#198038'},
            {from: 0, to: 0, color: 'rgba(0, 0, 0, 0.87)'},
            {from: 0, color: '#D12730'}
          ],
          colorFunction: ''
        },
        showArrow: true
      } as AggregatedValueCardKeySettings
    },
    {
      name: keyName, label: 'Delta absolute', type: DataKeyType.timeseries, units, decimals: 1,
      aggregationType: AggregationType.AVG,
      comparisonEnabled: true,
      timeForComparison: 'previousInterval',
      comparisonResultType: ComparisonResultType.DELTA_ABSOLUTE,
      settings: {
        position: AggregatedValueCardKeyPosition.rightBottom,
        font: {
          family: 'Roboto',
          size: 11,
          sizeUnit: 'px',
          style: 'normal',
          weight: '400',
          lineHeight: '1'
        },
        color: constantColor('rgba(0, 0, 0, 0.38)'),
        showArrow: false
      } as AggregatedValueCardKeySettings
    }
  ];

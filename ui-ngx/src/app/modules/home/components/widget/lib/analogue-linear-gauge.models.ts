///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { JsonSettingsSchema } from '@shared/models/widget.models';
import { AnalogueGaugeSettings, analogueGaugeSettingsSchema } from '@home/components/widget/lib/analogue-gauge.models';
import { deepClone } from '@core/utils';

export interface AnalogueLinearGaugeSettings extends AnalogueGaugeSettings {
  barStrokeWidth: number;
  colorBarStroke: string;
  colorBar: string;
  colorBarEnd: string;
  colorBarProgress: string;
  colorBarProgressEnd: string;
}

export function getAnalogueLinearGaugeSettingsSchema(): JsonSettingsSchema {
  const analogueLinearGaugeSettingsSchema = deepClone(analogueGaugeSettingsSchema);
  analogueLinearGaugeSettingsSchema.schema.properties =
    {...analogueLinearGaugeSettingsSchema.schema.properties, ...{
        barStrokeWidth: {
          title: 'Bar stroke width',
          type: 'number',
          default: 2.5
        },
        colorBarStroke: {
          title: 'Bar stroke color',
          type: 'string',
          default: null
        },
        colorBar: {
          title: 'Bar background color',
          type: 'string',
          default: '#fff'
        },
        colorBarEnd: {
          title: 'Bar background color - end gradient',
          type: 'string',
          default: '#ddd'
        },
        colorBarProgress: {
          title: 'Progress bar color',
          type: 'string',
          default: null
        },
        colorBarProgressEnd: {
          title: 'Progress bar color - end gradient',
          type: 'string',
          default: null
        }}};
  analogueLinearGaugeSettingsSchema.form.unshift(
    'barStrokeWidth',
    {
      key: 'colorBarStroke',
      type: 'color'
    },
    {
      key: 'colorBar',
      type: 'color'
    },
    {
      key: 'colorBarEnd',
      type: 'color'
    },
    {
      key: 'colorBarProgress',
      type: 'color'
    },
    {
      key: 'colorBarProgressEnd',
      type: 'color'
    }
  );
  return analogueLinearGaugeSettingsSchema;
}

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
import { FontSettings } from '@home/components/widget/lib/settings.models';
import { AnimationRule, AnimationTarget } from '@home/components/widget/lib/analogue-gauge.models';

export interface AnalogueCompassSettings {
  majorTicks: string[];
  minorTicks: number;
  showStrokeTicks: boolean;
  needleCircleSize: number;
  showBorder: boolean;
  borderOuterWidth: number;
  colorPlate: string;
  colorMajorTicks: string;
  colorMinorTicks: string;
  colorNeedle: string;
  colorNeedleCircle: string;
  colorBorder: string;
  majorTickFont: FontSettings;
  animation: boolean;
  animationDuration: number;
  animationRule: AnimationRule;
  animationTarget: AnimationTarget;
}

export const analogueCompassSettingsSchema: JsonSettingsSchema = {
  schema: {
    type: 'object',
    title: 'Settings',
    properties: {
      majorTicks: {
        title: 'Major ticks names',
        type: 'array',
        items: {
          title: 'Tick name',
          type: 'string'
        }
      },
      minorTicks: {
        title: 'Minor ticks count',
        type: 'number',
        default: 22
      },
      showStrokeTicks: {
        title: 'Show ticks stroke',
        type: 'boolean',
        default: false
      },
      needleCircleSize: {
        title: 'Needle circle size',
        type: 'number',
        default: 15
      },
      showBorder: {
        title: 'Show border',
        type: 'boolean',
        default: true
      },
      borderOuterWidth: {
        title: 'Border width',
        type: 'number',
        default: 10
      },
      colorPlate: {
        title: 'Plate color',
        type: 'string',
        default: '#222'
      },
      colorMajorTicks: {
        title: 'Major ticks color',
        type: 'string',
        default: '#f5f5f5'
      },
      colorMinorTicks: {
        title: 'Minor ticks color',
        type: 'string',
        default: '#ddd'
      },
      colorNeedle: {
        title: 'Needle color',
        type: 'string',
        default: '#f08080'
      },
      colorNeedleCircle: {
        title: 'Needle circle color',
        type: 'string',
        default: '#e8e8e8'
      },
      colorBorder: {
        title: 'Border color',
        type: 'string',
        default: '#ccc'
      },
      majorTickFont: {
        title: 'Major tick font',
        type: 'object',
        properties: {
          family: {
            title: 'Font family',
            type: 'string',
            default: 'Roboto'
          },
          size: {
            title: 'Size',
            type: 'number',
            default: 20
          },
          style: {
            title: 'Style',
            type: 'string',
            default: 'normal'
          },
          weight: {
            title: 'Weight',
            type: 'string',
            default: '500'
          },
          color: {
            title: 'color',
            type: 'string',
            default: '#ccc'
          }
        }
      },
      animation: {
        title: 'Enable animation',
        type: 'boolean',
        default: true
      },
      animationDuration: {
        title: 'Animation duration',
        type: 'number',
        default: 500
      },
      animationRule: {
        title: 'Animation rule',
        type: 'string',
        default: 'cycle'
      },
      animationTarget: {
        title: 'Animation target',
        type: 'string',
        default: 'needle'
      }
    },
    required: []
  },
  form: [
    {
      key: 'majorTicks',
      items:[
        'majorTicks[]'
      ]
    },
    'minorTicks',
    'showStrokeTicks',
    'needleCircleSize',
    'showBorder',
    'borderOuterWidth',
    {
      key: 'colorPlate',
      type: 'color'
    },
    {
      key: 'colorMajorTicks',
      type: 'color'
    },
    {
      key: 'colorMinorTicks',
      type: 'color'
    },
    {
      key: 'colorNeedle',
      type: 'color'
    },
    {
      key: 'colorNeedleCircle',
      type: 'color'
    },
    {
      key: 'colorBorder',
      type: 'color'
    },
    {
      key: 'majorTickFont',
      items: [
        'majorTickFont.family',
        'majorTickFont.size',
        {
          key: 'majorTickFont.style',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'normal',
              label: 'Normal'
            },
            {
              value: 'italic',
              label: 'Italic'
            },
            {
              value: 'oblique',
              label: 'Oblique'
            }
          ]
        },
        {
          key: 'majorTickFont.weight',
          type: 'rc-select',
          multiple: false,
          items: [
            {
              value: 'normal',
              label: 'Normal'
            },
            {
              value: 'bold',
              label: 'Bold'
            },
            {
              value: 'bolder',
              label: 'Bolder'
            },
            {
              value: 'lighter',
              label: 'Lighter'
            },
            {
              value: '100',
              label: '100'
            },
            {
              value: '200',
              label: '200'
            },
            {
              value: '300',
              label: '300'
            },
            {
              value: '400',
              label: '400'
            },
            {
              value: '500',
              label: '500'
            },
            {
              value: '600',
              label: '600'
            },
            {
              value: '700',
              label: '800'
            },
            {
              value: '800',
              label: '800'
            },
            {
              value: '900',
              label: '900'
            }
          ]
        },
        {
          key: 'majorTickFont.color',
          type: 'color'
        }
      ]
    },
    'animation',
    'animationDuration',
    {
      key: 'animationRule',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'linear',
          label: 'Linear'
        },
        {
          value: 'quad',
          label: 'Quad'
        },
        {
          value: 'quint',
          label: 'Quint'
        },
        {
          value: 'cycle',
          label: 'Cycle'
        },
        {
          value: 'bounce',
          label: 'Bounce'
        },
        {
          value: 'elastic',
          label: 'Elastic'
        },
        {
          value: 'dequad',
          label: 'Dequad'
        },
        {
          value: 'dequint',
          label: 'Dequint'
        },
        {
          value: 'decycle',
          label: 'Decycle'
        },
        {
          value: 'debounce',
          label: 'Debounce'
        },
        {
          value: 'delastic',
          label: 'Delastic'
        }
      ]
    },
    {
      key: 'animationTarget',
      type: 'rc-select',
      multiple: false,
      items: [
        {
          value: 'needle',
          label: 'Needle'
        },
        {
          value: 'plate',
          label: 'Plate'
        }
      ]
    }
  ]
};

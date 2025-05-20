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

import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type VolumeFlowUnits = VolumeFlowMetricUnits | VolumeFlowImperialUnits;

export type VolumeFlowMetricUnits =
  | 'dm³/s'
  | 'mL/min'
  | 'L/s'
  | 'L/min'
  | 'L/hr'
  | 'm³/s'
  | 'm³/hr';

export type VolumeFlowImperialUnits =
  | 'fl-oz/s'
  | 'ft³/s'
  | 'ft³/min'
  | 'gal/hr'
  | 'GPM';

const METRIC: TbMeasureUnits<VolumeFlowMetricUnits> = {
  ratio: 33.8140227,
  units: {
    'L/s': {
      name: 'unit.liter-per-second',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1,
    },
    'dm³/s': {
      name: 'unit.cubic-decimeter-per-second',
      tags: ['cubic decimeter per second'],
      to_anchor: 1,
    },
    'mL/min': {
      name: 'unit.milliliters-per-minute',
      tags: ['flow rate', 'fluid dynamics'],
      to_anchor: 1 / 60000,
    },
    'L/min': {
      name: 'unit.liter-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1 / 60,
    },
    'L/hr': {
      name: 'unit.liters-per-hour',
      tags: ['fuel consumption'],
      to_anchor: 1 / 3600,
    },
    'm³/s': {
      name: 'unit.cubic-meters-per-second',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1000,
    },
    'm³/hr': {
      name: 'unit.cubic-meters-per-hour',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 5 / 18,
    },
  },
};

const IMPERIAL: TbMeasureUnits<VolumeFlowImperialUnits> = {
  ratio: 1 / 33.8140227,
  units: {
    'fl-oz/s': {
      name: 'unit.fluid-ounce-per-second',
      tags: ['fluid ounce per second', 'fl-oz/s'],
      to_anchor: 1,
    },
    'ft³/s': {
      name: 'unit.cubic-foot-per-second',
      tags: ['flow rate', 'fluid flow'],
      to_anchor: 957.506,
    },
    'ft³/min': {
      name: 'unit.cubic-foot-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate', 'CFM', 'flow rate', 'fluid flow'],
      to_anchor: 957.506 / 60,
    },
    'gal/hr': {
      name: 'unit.gallons-per-hour',
      tags: ['fuel consumption'],
      to_anchor: 128 / 3600,
    },
    'GPM': {
      name: 'unit.gallons-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 128 / 60,
    },
  },
};

const measure: TbMeasure<VolumeFlowUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;

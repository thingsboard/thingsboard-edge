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

export type EnergyUnits = EnergyMetricUnits | EnergyImperialUnits;

export type EnergyMetricUnits =
  | 'Wm'
  | 'Wh'
  | 'mWh'
  | 'kWh'
  | 'MWh'
  | 'GWh'
  | 'μJ'
  | 'mJ'
  | 'J'
  | 'kJ'
  | 'MJ'
  | 'GJ'
  | 'eV';

export type EnergyImperialUnits = 'kcal' | 'cal' | 'Cal' | 'BTU' | 'MBtu' | 'MMBtu' | 'ft·lb' | 'thm';

const METRIC: TbMeasureUnits<EnergyMetricUnits> = {
  ratio: 1 / 4.184,
  units: {
    Wm: {
      name: 'unit.watt-minute',
      to_anchor: 60,
    },
    Wh: {
      name: 'unit.watt-hour',
      tags: ['energy usage', 'power consumption', 'energy consumption', 'electricity usage'],
      to_anchor: 3600,
    },
    mWh: {
      name: 'unit.milliwatt-hour',
      to_anchor: 3.6,
    },
    kWh: {
      name: 'unit.kilowatt-hour',
      tags: ['energy usage', 'power consumption', 'energy consumption', 'electricity usage'],
      to_anchor: 3600000,
    },
    MWh: {
      name: 'unit.megawatt-hour',
      to_anchor: 3600000000,
    },
    GWh: {
      name: 'unit.gigawatt-hour',
      to_anchor: 3600000000000,
    },
    μJ: {
      name: 'unit.microjoule',
      to_anchor: 0.000001,
    },
    mJ: {
      name: 'unit.millijoule',
      to_anchor: 0.001,
    },
    J: {
      name: 'unit.joule',
      tags: ['joule', 'joules', 'energy', 'work done', 'heat', 'electricity', 'mechanical work'],
      to_anchor: 1,
    },
    kJ: {
      name: 'unit.kilojoule',
      to_anchor: 1000,
    },
    MJ: {
      name: 'unit.megajoule',
      to_anchor: 1000000,
    },
    GJ: {
      name: 'unit.gigajoule',
      to_anchor: 1000000000,
    },
    eV: {
      name: 'unit.electron-volts',
      tags: ['subatomic particles', 'radiation'],
      to_anchor: 1.602176634e-19,
    },
  },
};

const IMPERIAL: TbMeasureUnits<EnergyImperialUnits> = {
  ratio: 4.184,
  units: {
    cal: {
      name: 'unit.small-calorie',
      to_anchor: 1,
    },
    Cal: {
      name: 'unit.calorie',
      tags: ['food energy'],
      to_anchor: 1000,
    },
    kcal: {
      name: 'unit.kilocalorie',
      tags: ['small calorie'],
      to_anchor: 1000,
    },
    BTU: {
      name: 'unit.british-thermal-unit',
      tags: ['heat', 'work done'],
      to_anchor: 252.1644007218,
    },
    MBtu: {
      name: 'unit.thousand-british-thermal-unit',
      tags: ['heat', 'work done'],
      to_anchor: 252164.4007218,
    },
    MMBtu : {
      name: 'unit.million-british-thermal-unit',
      tags: ['heat', 'work done'],
      to_anchor: 252164400.7218,
    },
    'ft·lb': {
      name: 'unit.foot-pound',
      tags: ['ft⋅lbf'],
      to_anchor: 0.32404875717017,
    },
    thm: {
      name: 'unit.therm',
      tags: ['natural gas consumption', 'BTU'],
      to_anchor: 25219021.687207,
    },
  },
};

const measure: TbMeasure<EnergyUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;

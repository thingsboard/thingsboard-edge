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

import { Injectable } from '@angular/core';
import moment from 'moment-timezone';
import {
  AllMeasures,
  AllMeasuresUnits,
  Converter,
  getUnitConverter,
  isTbUnitMapping,
  TbUnit,
  TbUnitConverter,
  TbUnitMapping,
  UnitInfo,
  UnitInfoGroupByMeasure,
  UnitSystem
} from '@shared/models/unit.models';
import { isNotEmptyStr, isObject } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuth, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { filter, switchMap, take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UnitService {

  private currentUnitSystem: UnitSystem = UnitSystem.METRIC;
  private converter: Converter;

  constructor(private translate: TranslateService,
              private store: Store<AppState>) {
    this.translate.onLangChange.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.converter = getUnitConverter(this.translate);
    });
    this.store.select(selectIsAuthenticated).pipe(
      filter((data) => data),
      switchMap(() => this.store.select(selectAuth).pipe(take(1)))
    ).subscribe((data) => {
      this.setUnitSystem(data.userDetails?.additionalInfo?.unitSystem)
    })
  }

  getUnitSystem(): UnitSystem {
    return this.currentUnitSystem;
  }

  setUnitSystem(unitSystem: UnitSystem) {
    if (isNotEmptyStr(unitSystem)) {
      this.currentUnitSystem = unitSystem;
    } else {
      this.currentUnitSystem = this.getUnitSystemByTimezone();
    }
  }

  getUnits(measure?: AllMeasures, unitSystem?: UnitSystem): UnitInfo[] {
    return this.converter?.listUnits(measure, unitSystem);
  }

  getUnitsGroupedByMeasure(measure?: AllMeasures, unitSystem?: UnitSystem, tagFilter?: string): UnitInfoGroupByMeasure<AllMeasures> {
    return this.converter?.unitsGroupByMeasure(measure, unitSystem, tagFilter);
  }

  getUnitInfo(symbol: AllMeasuresUnits | string): UnitInfo {
    return this.converter.describe(symbol);
  }

  getDefaultUnit(measure: AllMeasures, unitSystem: UnitSystem): AllMeasuresUnits {
    return this.converter.getDefaultUnit(measure, unitSystem);
  }

  geUnitConverter(unit: TbUnit): TbUnitConverter;
  geUnitConverter(from: string, to: string): TbUnitConverter;
  geUnitConverter(unit: TbUnit, to?: string): TbUnitConverter {
    try {
      if (isTbUnitMapping(unit)) {
        const target = this.getTargetUnitSymbol(unit);
        return this.converter.getUnitConverter((unit as TbUnitMapping).from, target);
      }
      if (isNotEmptyStr(to)) {
        return this.converter.getUnitConverter(unit as string, to);
      }
      return (x: number) => x;
    } catch (e) {
      console.warn(e);
      return (x: number) => x;
    }
  }

  getTargetUnitSymbol(unit: TbUnitMapping | string): string {
    if (isObject(unit)) {
      return isNotEmptyStr(unit[this.currentUnitSystem]) ? unit[this.currentUnitSystem] : (unit as TbUnitMapping).from;
    }
    return typeof unit === 'string' ? unit : null;
  }

  convertUnitValue(value: number, unit: TbUnit): number;
  convertUnitValue(value: number, from: string, to: string): number;
  convertUnitValue(value: number, unit: TbUnit, to?: string): number {
    try {
      if (isTbUnitMapping(unit)) {
        const target = this.getTargetUnitSymbol(unit);
        return this.converter.convert(value, (unit as TbUnitMapping).from, target);
      }
      if (isNotEmptyStr(to)) {
        return this.converter.convert(value, unit as string, to);
      }
      return value;
    } catch (e) {
      console.warn(e);
      return value;
    }
  }

  private getUnitSystemByTimezone(): UnitSystem {
    const timeZone = moment.tz.guess(true);
    const imperialCountries = ['US', 'LR', 'MM'];

    if (moment.tz.zonesForCountry('GB').includes(timeZone)) {
      return UnitSystem.HYBRID;
    }
    return imperialCountries.some(country =>
      moment.tz.zonesForCountry(country).includes(timeZone)
    ) ? UnitSystem.IMPERIAL : UnitSystem.METRIC;
  }
}

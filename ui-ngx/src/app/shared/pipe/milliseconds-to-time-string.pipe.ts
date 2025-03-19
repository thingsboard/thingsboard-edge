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

import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DAY, HOUR, MINUTE, SECOND, YEAR } from '@shared/models/time/time.models';

@Pipe({
  name: 'milliSecondsToTimeString'
})
export class MillisecondsToTimeStringPipe implements PipeTransform {

  constructor(private translate: TranslateService) {
  }

  transform(milliSeconds: number, shortFormat = false, onlyFirstDigit = false): string {
    const { years, days, hours, minutes, seconds } = this.extractTimeUnits(milliSeconds);
    return this.formatTimeString(years, days, hours, minutes, seconds, shortFormat, onlyFirstDigit);
  }

  private extractTimeUnits(milliseconds: number): { years: number; days: number; hours: number; minutes: number; seconds: number } {
    const years = Math.floor(milliseconds / YEAR);
    const days = Math.floor((milliseconds % YEAR) / DAY);
    const hours = Math.floor((milliseconds % DAY) / HOUR);
    const minutes = Math.floor((milliseconds % HOUR) / MINUTE);
    const seconds = Math.floor((milliseconds % MINUTE) / SECOND);
    return { years, days, hours, minutes, seconds };
  }

  private formatTimeString(
    years: number,
    days: number,
    hours: number,
    minutes: number,
    seconds: number,
    shortFormat: boolean,
    onlyFirstDigit: boolean
  ): string {
    const timeUnits = [
      { value: years, key: 'years', shortKey: 'short.years' },
      { value: days, key: 'days', shortKey: 'short.days' },
      { value: hours, key: 'hours', shortKey: 'short.hours' },
      { value: minutes, key: 'minutes', shortKey: 'short.minutes' },
      { value: seconds, key: 'seconds', shortKey: 'short.seconds' }
    ];

    let timeString = '';
    for (const { value, key, shortKey } of timeUnits) {
      if (value > 0) {
        timeString += this.translate.instant(shortFormat ? `timewindow.${shortKey}` : `timewindow.${key}`, { [key]: value });
        if (onlyFirstDigit) {
          return timeString.trim();
        }
      }
    }

    return timeString.length > 0 ? timeString : this.translate.instant('timewindow.short.seconds', { seconds: 0 });
  }
}

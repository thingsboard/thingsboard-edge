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

import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
  name: 'milliSecondsToTimeString'
})
export class MillisecondsToTimeStringPipe implements PipeTransform {

  constructor(private translate: TranslateService) {
  }

  transform(millseconds: number, shortFormat = false): string {
    let seconds = Math.floor(millseconds / 1000);
    const days = Math.floor(seconds / 86400);
    let hours = Math.floor((seconds % 86400) / 3600);
    let minutes = Math.floor(((seconds % 86400) % 3600) / 60);
    seconds = seconds % 60;
    let timeString = '';
    if (shortFormat) {
      if (days > 0) {
        timeString += this.translate.instant('timewindow.short.days', {days});
      }
      if (hours > 0) {
        timeString += this.translate.instant('timewindow.short.hours', {hours});
      }
      if (minutes > 0) {
        timeString += this.translate.instant('timewindow.short.minutes', {minutes});
      }
      if (seconds > 0) {
        timeString += this.translate.instant('timewindow.short.seconds', {seconds});
      }
      if (!timeString.length) {
        timeString += this.translate.instant('timewindow.short.seconds', {seconds: 0});
      }
    } else {
      if (days > 0) {
        timeString += this.translate.instant('timewindow.days', {days});
      }
      if (hours > 0) {
        if (timeString.length === 0 && hours === 1) {
          hours = 0;
        }
        timeString += this.translate.instant('timewindow.hours', {hours});
      }
      if (minutes > 0) {
        if (timeString.length === 0 && minutes === 1) {
          minutes = 0;
        }
        timeString += this.translate.instant('timewindow.minutes', {minutes});
      }
      if (seconds > 0) {
        if (timeString.length === 0 && seconds === 1) {
          seconds = 0;
        }
        timeString += this.translate.instant('timewindow.seconds', {seconds});
      }
    }
    return timeString;
  }
}

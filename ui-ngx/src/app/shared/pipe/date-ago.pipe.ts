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

import { Inject, Pipe, PipeTransform } from '@angular/core';
import { DAY, HOUR, MINUTE, SECOND, WEEK, YEAR } from '@shared/models/time/time.models';
import { TranslateService } from '@ngx-translate/core';

const intervals = {
  years: YEAR,
  months: DAY * 30,
  weeks: WEEK,
  days: DAY,
  hr: HOUR,
  min: MINUTE,
  sec: SECOND
};

@Pipe({
  name: 'dateAgo'
})
export class DateAgoPipe implements PipeTransform {

  constructor(@Inject(TranslateService) private translate: TranslateService) {

  }

  transform(value: number, args?: any): string {
    if (value) {
      const applyAgo = !!args?.applyAgo;
      const ms = Math.floor((+new Date() - +new Date(value)));
      if (ms < 29 * SECOND) { // less than 30 seconds ago will show as 'Just now'
        return this.translate.instant('timewindow.just-now');
      }
      let counter;
      // eslint-disable-next-line guard-for-in
      for (const i in intervals) {
        counter = Math.floor(ms / intervals[i]);
        if (counter > 0) {
          let res = this.translate.instant(`timewindow.${i}`, {[i]: counter});
          if (applyAgo) {
            res += ' ' + this.translate.instant('timewindow.ago');
          }
          return res;
        }
      }
    }
    return '';
  }

}

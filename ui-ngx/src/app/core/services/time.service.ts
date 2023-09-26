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

import { Injectable } from '@angular/core';
import {
  AggregationType,
  DAY,
  defaultTimeIntervals,
  defaultTimewindow,
  SECOND,
  Timewindow
} from '@shared/models/time/time.models';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptions } from '@core/http/http-utils';
import { map } from 'rxjs/operators';
import { isDefined } from '@core/utils';

export interface TimeInterval {
  name: string;
  translateParams: { [key: string]: any };
  value: number;
}

const MIN_INTERVAL = SECOND;
const MAX_INTERVAL = 365 * 20 * DAY;

const MIN_LIMIT = 7;

const MAX_DATAPOINTS_LIMIT = 500;

@Injectable({
  providedIn: 'root'
})
export class TimeService {

  private maxDatapointsLimit = MAX_DATAPOINTS_LIMIT;

  constructor(
    private http: HttpClient
  ) {}

  public setMaxDatapointsLimit(limit: number) {
    this.maxDatapointsLimit = limit;
    if (!this.maxDatapointsLimit || this.maxDatapointsLimit <= MIN_LIMIT) {
      this.maxDatapointsLimit = MIN_LIMIT + 1;
    }
  }

  public matchesExistingInterval(min: number, max: number, intervalMs: number): boolean {
    const intervals = this.getIntervals(min, max);
    return intervals.findIndex(interval => interval.value === intervalMs) > -1;
  }

  public getIntervals(min: number, max: number): Array<TimeInterval> {
    min = this.boundMinInterval(min);
    max = this.boundMaxInterval(max);
    return defaultTimeIntervals.filter((interval) => interval.value >= min && interval.value <= max);
  }

  public boundMinInterval(min: number): number {
    if (isDefined(min)) {
      min = Math.ceil(min / 1000) * 1000;
    }
    return this.toBound(min, MIN_INTERVAL, MAX_INTERVAL, MIN_INTERVAL);
  }

  public boundMaxInterval(max: number): number {
    if (isDefined(max)) {
      max = Math.floor(max / 1000) * 1000;
    }
    return this.toBound(max, MIN_INTERVAL, MAX_INTERVAL, MAX_INTERVAL);
  }

  public boundToPredefinedInterval(min: number, max: number, intervalMs: number): number {
    const intervals = this.getIntervals(min, max);
    let minDelta = MAX_INTERVAL;
    const boundedInterval = intervalMs || min;
    if (!intervals.length) {
      return boundedInterval;
    }
    let matchedInterval: TimeInterval = intervals[0];
    intervals.forEach((interval) => {
      const delta = Math.abs(interval.value - boundedInterval);
      if (delta < minDelta) {
        matchedInterval = interval;
        minDelta = delta;
      }
    });
    return matchedInterval.value;
  }

  public boundIntervalToTimewindow(timewindow: number, intervalMs: number, aggType: AggregationType): number {
    if (aggType === AggregationType.NONE) {
      return SECOND;
    } else {
      const min = this.minIntervalLimit(timewindow);
      const max = this.maxIntervalLimit(timewindow);
      if (intervalMs) {
        return this.toBound(intervalMs, min, max, intervalMs);
      } else {
        return this.boundToPredefinedInterval(min, max, this.avgInterval(timewindow));
      }
    }
  }

  public getMaxDatapointsLimit(): number {
    return this.maxDatapointsLimit;
  }

  public getMinDatapointsLimit(): number {
    return MIN_LIMIT;
  }

  public avgInterval(timewindow: number): number {
    const avg = timewindow / 200;
    return this.boundMinInterval(avg);
  }

  public minIntervalLimit(timewindowMs: number): number {
    const min = timewindowMs / 500;
    return this.boundMinInterval(min);
  }

  public maxIntervalLimit(timewindowMs: number): number {
    const max = timewindowMs / MIN_LIMIT;
    return this.boundMaxInterval(max);
  }

  public defaultTimewindow(): Timewindow {
    return defaultTimewindow(this);
  }

  private toBound(value: number, min: number, max: number, defValue: number): number {
    if (isDefined(value)) {
      value = Math.max(value, min);
      value = Math.min(value, max);
      return value;
    } else {
      return defValue;
    }
  }

}

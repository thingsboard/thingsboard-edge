///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { TimeService } from '@core/services/time.service';
import { deepClone, isDefined, isNumeric, isUndefined } from '@app/core/utils';
import * as moment_ from 'moment';
import * as momentTz from 'moment-timezone';
import { IntervalType } from '@shared/models/telemetry/telemetry.models';

const moment = moment_;

export const SECOND = 1000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;
export const WEEK = 7 * DAY;

export const AVG_MONTH = Math.floor(30.44 * DAY);

export const AVG_QUARTER = Math.floor(DAY * 365.2425 / 4);

export const YEAR = DAY * 365;

export type ComparisonDuration = moment_.unitOfTime.DurationConstructor | 'previousInterval' | 'customInterval';

export enum TimewindowType {
  REALTIME,
  HISTORY
}

export enum RealtimeWindowType {
  LAST_INTERVAL,
  INTERVAL
}

export enum HistoryWindowType {
  LAST_INTERVAL,
  FIXED,
  INTERVAL,
  FOR_ALL_TIME
}

export type Interval = number | IntervalType;

export class IntervalMath {
  public static max(...values: Interval[]): Interval {
    const numberArr = values.map(v => IntervalMath.numberValue(v));
    const index = numberArr.indexOf(Math.max(...numberArr));
    return values[index];
  }

  public static min(...values: Interval[]): Interval {
    const numberArr = values.map(v => IntervalMath.numberValue(v));
    const index = numberArr.indexOf(Math.min(...numberArr));
    return values[index];
  }

  public static numberValue(value: Interval): number {
    return typeof value === 'number' ? value : IntervalTypeValuesMap.get(value);
  }
}

export interface IntervalWindow {
  interval?: Interval;
  timewindowMs?: number;
  quickInterval?: QuickTimeInterval;
}

export interface RealtimeWindow extends IntervalWindow{
  realtimeType?: RealtimeWindowType;
}

export interface FixedWindow {
  startTimeMs: number;
  endTimeMs: number;
}

export interface HistoryWindow extends IntervalWindow {
  historyType?: HistoryWindowType;
  fixedTimewindow?: FixedWindow;
}

export enum AggregationType {
  MIN = 'MIN',
  MAX = 'MAX',
  AVG = 'AVG',
  SUM = 'SUM',
  COUNT = 'COUNT',
  NONE = 'NONE'
}

export const aggregationTranslations = new Map<AggregationType, string>(
  [
    [AggregationType.MIN, 'aggregation.min'],
    [AggregationType.MAX, 'aggregation.max'],
    [AggregationType.AVG, 'aggregation.avg'],
    [AggregationType.SUM, 'aggregation.sum'],
    [AggregationType.COUNT, 'aggregation.count'],
    [AggregationType.NONE, 'aggregation.none'],
  ]
);

export interface Aggregation {
  interval?: Interval;
  type: AggregationType;
  limit: number;
}

export interface Timewindow {
  displayValue?: string;
  displayTimezoneAbbr?: string;
  hideInterval?: boolean;
  hideQuickInterval?: boolean;
  hideLastInterval?: boolean;
  hideAggregation?: boolean;
  hideAggInterval?: boolean;
  hideTimezone?: boolean;
  selectedTab?: TimewindowType;
  realtime?: RealtimeWindow;
  history?: HistoryWindow;
  aggregation?: Aggregation;
  timezone?: string;
}

export interface SubscriptionAggregation extends Aggregation {
  interval?: Interval;
  timeWindow?: number;
  stateData?: boolean;
}

export interface SubscriptionTimewindow {
  startTs?: number;
  quickInterval?: QuickTimeInterval;
  timezone?: string;
  tsOffset?: number;
  realtimeWindowMs?: number;
  fixedWindow?: FixedWindow;
  aggregation?: SubscriptionAggregation;
  timeForComparison?: ComparisonDuration;
}

export interface WidgetTimewindow {
  minTime?: number;
  maxTime?: number;
  interval?: Interval;
  timezone?: string;
  tsOffset?: number;
  stDiff?: number;
}

export enum QuickTimeInterval {
  YESTERDAY = 'YESTERDAY',
  DAY_BEFORE_YESTERDAY = 'DAY_BEFORE_YESTERDAY',
  THIS_DAY_LAST_WEEK = 'THIS_DAY_LAST_WEEK',
  PREVIOUS_WEEK = 'PREVIOUS_WEEK',
  PREVIOUS_WEEK_ISO = 'PREVIOUS_WEEK_ISO',
  PREVIOUS_MONTH = 'PREVIOUS_MONTH',
  PREVIOUS_QUARTER = 'PREVIOUS_QUARTER',
  PREVIOUS_HALF_YEAR = 'PREVIOUS_HALF_YEAR',
  PREVIOUS_YEAR = 'PREVIOUS_YEAR',
  CURRENT_HOUR = 'CURRENT_HOUR',
  CURRENT_DAY = 'CURRENT_DAY',
  CURRENT_DAY_SO_FAR = 'CURRENT_DAY_SO_FAR',
  CURRENT_WEEK = 'CURRENT_WEEK',
  CURRENT_WEEK_ISO = 'CURRENT_WEEK_ISO',
  CURRENT_WEEK_SO_FAR = 'CURRENT_WEEK_SO_FAR',
  CURRENT_WEEK_ISO_SO_FAR = 'CURRENT_WEEK_ISO_SO_FAR',
  CURRENT_MONTH = 'CURRENT_MONTH',
  CURRENT_MONTH_SO_FAR = 'CURRENT_MONTH_SO_FAR',
  CURRENT_QUARTER = 'CURRENT_QUARTER',
  CURRENT_QUARTER_SO_FAR = 'CURRENT_QUARTER_SO_FAR',
  CURRENT_HALF_YEAR = 'CURRENT_HALF_YEAR',
  CURRENT_HALF_YEAR_SO_FAR = 'CURRENT_HALF_YEAR_SO_FAR',
  CURRENT_YEAR = 'CURRENT_YEAR',
  CURRENT_YEAR_SO_FAR = 'CURRENT_YEAR_SO_FAR'
}

export const QuickTimeIntervalTranslationMap = new Map<QuickTimeInterval, string>([
  [QuickTimeInterval.YESTERDAY, 'timeinterval.predefined.yesterday'],
  [QuickTimeInterval.DAY_BEFORE_YESTERDAY, 'timeinterval.predefined.day-before-yesterday'],
  [QuickTimeInterval.THIS_DAY_LAST_WEEK, 'timeinterval.predefined.this-day-last-week'],
  [QuickTimeInterval.PREVIOUS_WEEK, 'timeinterval.predefined.previous-week'],
  [QuickTimeInterval.PREVIOUS_WEEK_ISO, 'timeinterval.predefined.previous-week-iso'],
  [QuickTimeInterval.PREVIOUS_MONTH, 'timeinterval.predefined.previous-month'],
  [QuickTimeInterval.PREVIOUS_QUARTER, 'timeinterval.predefined.previous-quarter'],
  [QuickTimeInterval.PREVIOUS_HALF_YEAR, 'timeinterval.predefined.previous-half-year'],
  [QuickTimeInterval.PREVIOUS_YEAR, 'timeinterval.predefined.previous-year'],
  [QuickTimeInterval.CURRENT_HOUR, 'timeinterval.predefined.current-hour'],
  [QuickTimeInterval.CURRENT_DAY, 'timeinterval.predefined.current-day'],
  [QuickTimeInterval.CURRENT_DAY_SO_FAR, 'timeinterval.predefined.current-day-so-far'],
  [QuickTimeInterval.CURRENT_WEEK, 'timeinterval.predefined.current-week'],
  [QuickTimeInterval.CURRENT_WEEK_ISO, 'timeinterval.predefined.current-week-iso'],
  [QuickTimeInterval.CURRENT_WEEK_SO_FAR, 'timeinterval.predefined.current-week-so-far'],
  [QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR, 'timeinterval.predefined.current-week-iso-so-far'],
  [QuickTimeInterval.CURRENT_MONTH, 'timeinterval.predefined.current-month'],
  [QuickTimeInterval.CURRENT_MONTH_SO_FAR, 'timeinterval.predefined.current-month-so-far'],
  [QuickTimeInterval.CURRENT_QUARTER, 'timeinterval.predefined.current-quarter'],
  [QuickTimeInterval.CURRENT_QUARTER_SO_FAR, 'timeinterval.predefined.current-quarter-so-far'],
  [QuickTimeInterval.CURRENT_HALF_YEAR, 'timeinterval.predefined.current-half-year'],
  [QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR, 'timeinterval.predefined.current-half-year-so-far'],
  [QuickTimeInterval.CURRENT_YEAR, 'timeinterval.predefined.current-year'],
  [QuickTimeInterval.CURRENT_YEAR_SO_FAR, 'timeinterval.predefined.current-year-so-far']
]);

export const IntervalTypeValuesMap = new Map<IntervalType, number>([
  [IntervalType.WEEK, WEEK],
  [IntervalType.WEEK_ISO, WEEK],
  [IntervalType.MONTH, AVG_MONTH],
  [IntervalType.QUARTER, AVG_QUARTER]
]);

export const forAllTimeInterval = (): Timewindow => ({
  selectedTab: TimewindowType.HISTORY,
  history: {
    historyType: HistoryWindowType.FOR_ALL_TIME
  }
});

export const historyInterval = (timewindowMs: number): Timewindow => ({
  selectedTab: TimewindowType.HISTORY,
  history: {
    historyType: HistoryWindowType.LAST_INTERVAL,
    timewindowMs
  }
});

export const defaultTimewindow = (timeService: TimeService): Timewindow => {
  const currentTime = moment().valueOf();
  return {
    displayValue: '',
    hideInterval: false,
    hideLastInterval: false,
    hideQuickInterval: false,
    hideAggregation: false,
    hideAggInterval: false,
    hideTimezone: false,
    selectedTab: TimewindowType.REALTIME,
    realtime: {
      realtimeType: RealtimeWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      quickInterval: QuickTimeInterval.CURRENT_DAY
    },
    history: {
      historyType: HistoryWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      fixedTimewindow: {
        startTimeMs: currentTime - DAY,
        endTimeMs: currentTime
      },
      quickInterval: QuickTimeInterval.CURRENT_DAY
    },
    aggregation: {
      type: AggregationType.AVG,
      limit: Math.floor(timeService.getMaxDatapointsLimit() / 2)
    }
  };
};

const getTimewindowType = (timewindow: Timewindow): TimewindowType => {
  if (isUndefined(timewindow.selectedTab)) {
    return isDefined(timewindow.realtime) ? TimewindowType.REALTIME : TimewindowType.HISTORY;
  } else {
    return timewindow.selectedTab;
  }
};

export const initModelFromDefaultTimewindow = (value: Timewindow, quickIntervalOnly: boolean,
                                               historyOnly: boolean, timeService: TimeService): Timewindow => {
  const model = defaultTimewindow(timeService);
  if (value) {
    model.hideInterval = value.hideInterval;
    model.hideLastInterval = value.hideLastInterval;
    model.hideQuickInterval = value.hideQuickInterval;
    model.hideAggregation = value.hideAggregation;
    model.hideAggInterval = value.hideAggInterval;
    model.hideTimezone = value.hideTimezone;
    model.selectedTab = getTimewindowType(value);
    if (isDefined(value.realtime)) {
      if (isDefined(value.realtime.interval)) {
        model.realtime.interval = value.realtime.interval;
      }
      if (isUndefined(value.realtime.realtimeType)) {
        if (isDefined(value.realtime.quickInterval)) {
          model.realtime.realtimeType = RealtimeWindowType.INTERVAL;
        } else {
          model.realtime.realtimeType = RealtimeWindowType.LAST_INTERVAL;
        }
      } else {
        model.realtime.realtimeType = value.realtime.realtimeType;
      }
      if (isDefined(value.realtime.quickInterval)) {
        model.realtime.quickInterval = value.realtime.quickInterval;
      }
      if (isDefined(value.realtime.timewindowMs)) {
        model.realtime.timewindowMs = value.realtime.timewindowMs;
      }
    }
    if (isDefined(value.history)) {
      if (isDefined(value.history.interval)) {
        model.history.interval = value.history.interval;
      }
      if (isUndefined(value.history.historyType)) {
        if (isDefined(value.history.timewindowMs)) {
          model.history.historyType = HistoryWindowType.LAST_INTERVAL;
        } else if (isDefined(value.history.quickInterval)) {
          model.history.historyType = HistoryWindowType.INTERVAL;
        } else {
          model.history.historyType = HistoryWindowType.FIXED;
        }
      } else {
        model.history.historyType = value.history.historyType;
      }
      if (isDefined(value.history.timewindowMs)) {
        model.history.timewindowMs = value.history.timewindowMs;
      }
      if (isDefined(value.history.quickInterval)) {
        model.history.quickInterval = value.history.quickInterval;
      }
      if (isDefined(value.history.fixedTimewindow)) {
        if (isDefined(value.history.fixedTimewindow.startTimeMs)) {
          model.history.fixedTimewindow.startTimeMs = value.history.fixedTimewindow.startTimeMs;
        }
        if (isDefined(value.history.fixedTimewindow.endTimeMs)) {
          model.history.fixedTimewindow.endTimeMs = value.history.fixedTimewindow.endTimeMs;
        }
      }
    }
    if (value.aggregation) {
      if (value.aggregation.type) {
        model.aggregation.type = value.aggregation.type;
      }
      model.aggregation.limit = value.aggregation.limit || Math.floor(timeService.getMaxDatapointsLimit() / 2);
    }
    model.timezone = value.timezone;
  }
  if (quickIntervalOnly) {
    model.realtime.realtimeType = RealtimeWindowType.INTERVAL;
  }
  if (historyOnly) {
    model.selectedTab = TimewindowType.HISTORY;
  }
  return model;
};

export const toHistoryTimewindow = (timewindow: Timewindow, startTimeMs: number, endTimeMs: number,
                                    interval: Interval, timeService: TimeService): Timewindow => {
  if (timewindow.history) {
    interval = isDefined(interval) ? interval : timewindow.history.interval;
  } else if (timewindow.realtime) {
    interval = timewindow.realtime.interval;
  }  else {
    interval = 0;
  }
  let aggType: AggregationType;
  let limit: number;
  if (timewindow.aggregation) {
    aggType = timewindow.aggregation.type || AggregationType.AVG;
    limit = timewindow.aggregation.limit || timeService.getMaxDatapointsLimit();
  } else {
    aggType = AggregationType.AVG;
    limit = timeService.getMaxDatapointsLimit();
  }
  return {
    hideInterval: timewindow.hideInterval || false,
    hideLastInterval: timewindow.hideLastInterval || false,
    hideQuickInterval: timewindow.hideQuickInterval || false,
    hideAggregation: timewindow.hideAggregation || false,
    hideAggInterval: timewindow.hideAggInterval || false,
    hideTimezone: timewindow.hideTimezone || false,
    selectedTab: TimewindowType.HISTORY,
    history: {
      historyType: HistoryWindowType.FIXED,
      fixedTimewindow: {
        startTimeMs,
        endTimeMs
      },
      interval: timeService.boundIntervalToTimewindow(endTimeMs - startTimeMs, interval, AggregationType.AVG)
    },
    aggregation: {
      type: aggType,
      limit
    },
    timezone: timewindow.timezone
  };
};

export const timewindowTypeChanged = (newTimewindow: Timewindow, oldTimewindow: Timewindow): boolean => {
  if (!newTimewindow || !oldTimewindow) {
    return false;
  }
  const newType = getTimewindowType(newTimewindow);
  const oldType = getTimewindowType(oldTimewindow);
  return newType !== oldType;
};

export const getTimezone = (tz: string): moment_.Moment => moment.tz(tz);

export const calculateTsOffset = (timezone?: string): number => {
  if (timezone) {
    const tz = getTimezone(timezone);
    const localOffset = moment().utcOffset();
    return (tz.utcOffset() - localOffset) * 60 * 1000;
  } else {
    return 0;
  }
};

export const isHistoryTypeTimewindow = (timewindow: Timewindow): boolean => getTimewindowType(timewindow) === TimewindowType.HISTORY;

export const getCurrentTime = (tz?: string): moment_.Moment => {
  if (tz) {
    return moment().tz(tz);
  } else {
    return moment();
  }
};

const getSubscriptionRealtimeWindowFromTimeInterval = (interval: QuickTimeInterval, tz?: string): number => {
  let currentDate;
  switch (interval) {
    case QuickTimeInterval.CURRENT_HOUR:
      return HOUR;
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return DAY;
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return WEEK;
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      currentDate = getCurrentTime(tz);
      return currentDate.clone().endOf('month').add(1, 'milliseconds').diff(currentDate.clone().startOf('month'));
    case QuickTimeInterval.CURRENT_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER_SO_FAR:
      currentDate = getCurrentTime(tz);
      return currentDate.clone().endOf('quarter').add(1, 'milliseconds').diff(currentDate.clone().startOf('quarter'));
    case QuickTimeInterval.CURRENT_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR:
      currentDate = getCurrentTime(tz);
      if (currentDate.get('quarter') < 3) {
        return currentDate.clone().set('quarter', 2).endOf('quarter').add(1, 'milliseconds').diff(currentDate.clone().startOf('year'));
      } else {
        return currentDate.clone().endOf('year').add(1, 'milliseconds').diff(currentDate.clone().set('quarter', 3).startOf('quarter'));
      }
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      currentDate = getCurrentTime(tz);
      return currentDate.clone().endOf('year').add(1, 'milliseconds').diff(currentDate.clone().startOf('year'));
  }
};

export const calculateIntervalStartTime = (interval: QuickTimeInterval, currentDate: moment_.Moment): moment_.Moment => {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
      currentDate.subtract(1, 'days');
      return currentDate.startOf('day');
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
      currentDate.subtract(2, 'days');
      return currentDate.startOf('day');
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
      currentDate.subtract(1, 'weeks');
      return currentDate.startOf('day');
    case QuickTimeInterval.PREVIOUS_WEEK:
      currentDate.subtract(1, 'weeks');
      return currentDate.startOf('week');
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
      currentDate.subtract(1, 'weeks');
      return currentDate.startOf('isoWeek');
    case QuickTimeInterval.PREVIOUS_MONTH:
      currentDate.subtract(1, 'months');
      return currentDate.startOf('month');
    case QuickTimeInterval.PREVIOUS_QUARTER:
      currentDate.subtract(1, 'quarter');
      return currentDate.startOf('quarter');
    case QuickTimeInterval.PREVIOUS_HALF_YEAR:
      if (currentDate.get('quarter') < 3) {
        return currentDate.startOf('year').subtract(2, 'quarters');
      } else {
        return currentDate.startOf('year');
      }
    case QuickTimeInterval.PREVIOUS_YEAR:
      currentDate.subtract(1, 'years');
      return currentDate.startOf('year');
    case QuickTimeInterval.CURRENT_HOUR:
      return currentDate.startOf('hour');
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return currentDate.startOf('day');
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
      return currentDate.startOf('week');
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return currentDate.startOf('isoWeek');
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return currentDate.startOf('month');
    case QuickTimeInterval.CURRENT_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER_SO_FAR:
      return currentDate.startOf('quarter');
    case QuickTimeInterval.CURRENT_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR:
      if (currentDate.get('quarter') < 3) {
        return currentDate.startOf('year');
      } else {
        return currentDate.clone().set('quarter', 3).startOf('quarter');
      }
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return currentDate.startOf('year');
  }
};

export const calculateIntervalEndTime = (interval: QuickTimeInterval, startDate: moment_.Moment, tz?: string): number => {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
    case QuickTimeInterval.CURRENT_DAY:
      return startDate.add(1, 'day').valueOf();
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
      return startDate.add(1, 'week').valueOf();
    case QuickTimeInterval.PREVIOUS_MONTH:
    case QuickTimeInterval.CURRENT_MONTH:
      return startDate.add(1, 'month').valueOf();
    case QuickTimeInterval.PREVIOUS_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER:
      return startDate.add(1, 'quarter').valueOf();
    case QuickTimeInterval.PREVIOUS_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR:
      return startDate.add(2, 'quarters').valueOf();
    case QuickTimeInterval.PREVIOUS_YEAR:
    case QuickTimeInterval.CURRENT_YEAR:
      return startDate.add(1, 'year').valueOf();
    case QuickTimeInterval.CURRENT_HOUR:
      return startDate.add(1, 'hour').valueOf();
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
    case QuickTimeInterval.CURRENT_QUARTER_SO_FAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return getCurrentTime(tz).valueOf();
  }
};

export const calculateIntervalStartEndTime = (interval: QuickTimeInterval, tz?: string): [number, number] => {
  const startEndTs: [number, number] = [0, 0];
  const currentDate = getCurrentTime(tz);
  const startDate = calculateIntervalStartTime(interval, currentDate);
  startEndTs[0] = startDate.valueOf();
  const endDate = calculateIntervalEndTime(interval, startDate, tz);
  startEndTs[1] = endDate.valueOf();
  return startEndTs;
};

export const createSubscriptionTimewindow = (timewindow: Timewindow, stDiff: number, stateData: boolean,
                                             timeService: TimeService): SubscriptionTimewindow => {
  const subscriptionTimewindow: SubscriptionTimewindow = {
    fixedWindow: null,
    realtimeWindowMs: null,
    aggregation: {
      interval: SECOND,
      limit: timeService.getMaxDatapointsLimit(),
      type: AggregationType.AVG
    },
    timezone: timewindow.timezone || getDefaultTimezone(),
    tsOffset: calculateTsOffset(timewindow.timezone)
  };
  let aggTimewindow: number;
  if (stateData) {
    subscriptionTimewindow.aggregation.type = AggregationType.NONE;
    subscriptionTimewindow.aggregation.stateData = true;
  }
  if (isDefined(timewindow.aggregation) && !stateData) {
    subscriptionTimewindow.aggregation = {
      type: timewindow.aggregation.type || AggregationType.AVG,
      limit: timewindow.aggregation.limit || timeService.getMaxDatapointsLimit()
    };
  }
  const selectedTab = getTimewindowType(timewindow);
  if (selectedTab === TimewindowType.REALTIME) {
    let realtimeType = timewindow.realtime.realtimeType;
    if (isUndefined(realtimeType)) {
      if (isDefined(timewindow.realtime.quickInterval)) {
        realtimeType = RealtimeWindowType.INTERVAL;
      } else {
        realtimeType = RealtimeWindowType.LAST_INTERVAL;
      }
    }
    if (realtimeType === RealtimeWindowType.INTERVAL) {
      subscriptionTimewindow.realtimeWindowMs =
        getSubscriptionRealtimeWindowFromTimeInterval(timewindow.realtime.quickInterval, timewindow.timezone);
      subscriptionTimewindow.quickInterval = timewindow.realtime.quickInterval;
      const currentDate = getCurrentTime(timewindow.timezone);
      subscriptionTimewindow.startTs = calculateIntervalStartTime(timewindow.realtime.quickInterval, currentDate).valueOf();
    } else {
      subscriptionTimewindow.realtimeWindowMs = timewindow.realtime.timewindowMs;
      const currentDate = getCurrentTime(timewindow.timezone);
      subscriptionTimewindow.startTs = currentDate.valueOf() + stDiff - subscriptionTimewindow.realtimeWindowMs;
    }
    subscriptionTimewindow.aggregation.interval =
      subscriptionTimewindow.aggregation.type === AggregationType.NONE
      ? SECOND
      : (!!timewindow.realtime.interval ? timewindow.realtime.interval :
          timeService.boundIntervalToTimewindow(subscriptionTimewindow.realtimeWindowMs, timewindow.realtime.interval,
              subscriptionTimewindow.aggregation.type));

    aggTimewindow = subscriptionTimewindow.realtimeWindowMs;
  } else {
    let historyType = timewindow.history.historyType;
    if (isUndefined(historyType)) {
      if (isDefined(timewindow.history.timewindowMs)) {
        historyType = HistoryWindowType.LAST_INTERVAL;
      } else if (isDefined(timewindow.history.quickInterval)) {
        historyType = HistoryWindowType.INTERVAL;
      } else {
        historyType = HistoryWindowType.FIXED;
      }
    }
    if (historyType === HistoryWindowType.LAST_INTERVAL) {
      const currentDate = getCurrentTime(timewindow.timezone);
      const currentTime = currentDate.valueOf();
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: currentTime - timewindow.history.timewindowMs,
        endTimeMs: currentTime
      };
      aggTimewindow = timewindow.history.timewindowMs;
    } else if (historyType === HistoryWindowType.INTERVAL) {
      const startEndTime = calculateIntervalStartEndTime(timewindow.history.quickInterval, timewindow.timezone);
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: startEndTime[0],
        endTimeMs: startEndTime[1]
      };
      aggTimewindow = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
      subscriptionTimewindow.quickInterval = timewindow.history.quickInterval;
    } else {
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: timewindow.history.fixedTimewindow.startTimeMs - subscriptionTimewindow.tsOffset,
        endTimeMs: timewindow.history.fixedTimewindow.endTimeMs - subscriptionTimewindow.tsOffset
      };
      aggTimewindow = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
    }
    subscriptionTimewindow.startTs = subscriptionTimewindow.fixedWindow.startTimeMs;
    subscriptionTimewindow.aggregation.interval =
      subscriptionTimewindow.aggregation.type === AggregationType.NONE
      ? SECOND
      : (!!timewindow.history.interval ? timewindow.history.interval :
          timeService.boundIntervalToTimewindow(aggTimewindow, timewindow.history.interval,
            subscriptionTimewindow.aggregation.type));
  }
  const aggregation = subscriptionTimewindow.aggregation;
  aggregation.timeWindow = aggTimewindow;
  if (aggregation.type !== AggregationType.NONE) {
    aggregation.limit = calculateIntervalsCount(subscriptionTimewindow.startTs, aggTimewindow,
      subscriptionTimewindow.aggregation.interval, timewindow.timezone);
  }
  return subscriptionTimewindow;
};

export const quickTimeIntervalPeriod = (interval: QuickTimeInterval): number => {
  switch (interval) {
    case QuickTimeInterval.CURRENT_HOUR:
      return HOUR;
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return DAY;
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return WEEK;
    case QuickTimeInterval.PREVIOUS_MONTH:
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return DAY * 30;
    case QuickTimeInterval.PREVIOUS_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER_SO_FAR:
      return DAY * 30 * 3;
    case QuickTimeInterval.PREVIOUS_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR:
      return DAY * 30 * 6;
    case QuickTimeInterval.PREVIOUS_YEAR:
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return YEAR;
  }
};

export const calculateIntervalComparisonStartTime = (interval: QuickTimeInterval,
                                                     startDate: moment_.Moment): moment_.Moment => {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      startDate.subtract(1, 'days');
      return startDate.startOf('day');
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
      startDate.subtract(1, 'weeks');
      return startDate.startOf('day');
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
      startDate.subtract(1, 'weeks');
      return startDate.startOf('week');
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      startDate.subtract(1, 'weeks');
      return startDate.startOf('isoWeek');
    case QuickTimeInterval.PREVIOUS_MONTH:
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      startDate.subtract(1, 'months');
      return startDate.startOf('month');
    case QuickTimeInterval.PREVIOUS_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER:
    case QuickTimeInterval.CURRENT_QUARTER_SO_FAR:
      startDate.subtract(1, 'quarters');
      return startDate.startOf('quarter');
    case QuickTimeInterval.PREVIOUS_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR:
    case QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR:
      startDate.subtract(2, 'quarters');
      if (startDate.get('quarter') < 3) {
        return startDate.startOf('year');
      } else {
        return startDate.clone().set('quarter', 3).startOf('quarter');
      }
    case QuickTimeInterval.PREVIOUS_YEAR:
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      startDate.subtract(1, 'years');
      return startDate.startOf('year');
    case QuickTimeInterval.CURRENT_HOUR:
      startDate.subtract(1, 'hour');
      return startDate.startOf('hour');
  }
};

export const calculateIntervalComparisonEndTime = (interval: QuickTimeInterval,
                                                   comparisonStartDate: moment_.Moment,
                                                   endDate: moment_.Moment): number => {
  switch (interval) {
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return endDate.subtract(1, 'days').valueOf();
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return endDate.subtract(1, 'week').valueOf();
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return endDate.subtract(1, 'month').valueOf();
    case QuickTimeInterval.CURRENT_QUARTER_SO_FAR:
      return endDate.subtract(1, 'quarter').valueOf();
    case QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR:
      return endDate.subtract(2, 'quarters').valueOf();
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return endDate.subtract(1, 'year').valueOf();
    default:
      return calculateIntervalEndTime(interval, comparisonStartDate);
  }
};

export const createTimewindowForComparison = (subscriptionTimewindow: SubscriptionTimewindow,
                                              timeUnit: ComparisonDuration, customIntervalValue: number): SubscriptionTimewindow => {
  const timewindowForComparison: SubscriptionTimewindow = {
    fixedWindow: null,
    realtimeWindowMs: null,
    aggregation: subscriptionTimewindow.aggregation,
    tsOffset: subscriptionTimewindow.tsOffset,
    timezone: subscriptionTimewindow.timezone
  };

  if (subscriptionTimewindow.fixedWindow) {
    let startTimeMs: number;
    let endTimeMs: number;
    if (timeUnit === 'previousInterval') {
      if (subscriptionTimewindow.quickInterval) {
        const startDate = moment(subscriptionTimewindow.fixedWindow.startTimeMs);
        const endDate = moment(subscriptionTimewindow.fixedWindow.endTimeMs);
        if (subscriptionTimewindow.timezone) {
          startDate.tz(subscriptionTimewindow.timezone);
          endDate.tz(subscriptionTimewindow.timezone);
        }
        const comparisonStartDate = calculateIntervalComparisonStartTime(subscriptionTimewindow.quickInterval, startDate);
        startTimeMs = comparisonStartDate.valueOf();
        endTimeMs = calculateIntervalComparisonEndTime(subscriptionTimewindow.quickInterval, comparisonStartDate, endDate);
      } else {
        const timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
        endTimeMs = subscriptionTimewindow.fixedWindow.startTimeMs;
        startTimeMs = endTimeMs - timeInterval;
      }
    } else if (timeUnit === 'customInterval') {
      if (isNumeric(customIntervalValue) && isFinite(customIntervalValue) && customIntervalValue > 0) {
        const timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
        endTimeMs = subscriptionTimewindow.fixedWindow.endTimeMs - Math.round(customIntervalValue);
        startTimeMs = endTimeMs - timeInterval;
      } else {
        endTimeMs = subscriptionTimewindow.fixedWindow.endTimeMs;
        startTimeMs = subscriptionTimewindow.fixedWindow.startTimeMs;
      }
    } else {
      const timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
      endTimeMs = moment(subscriptionTimewindow.fixedWindow.endTimeMs).subtract(1, timeUnit).valueOf();
      startTimeMs = endTimeMs - timeInterval;
    }
    timewindowForComparison.startTs = startTimeMs;
    timewindowForComparison.fixedWindow = {
      startTimeMs: timewindowForComparison.startTs,
      endTimeMs
    };
  }

  return timewindowForComparison;
};

export const cloneSelectedTimewindow = (timewindow: Timewindow): Timewindow => {
  const cloned: Timewindow = {};
  cloned.hideInterval = timewindow.hideInterval || false;
  cloned.hideLastInterval = timewindow.hideLastInterval || false;
  cloned.hideQuickInterval = timewindow.hideQuickInterval || false;
  cloned.hideAggregation = timewindow.hideAggregation || false;
  cloned.hideAggInterval = timewindow.hideAggInterval || false;
  cloned.hideTimezone = timewindow.hideTimezone || false;
  if (isDefined(timewindow.selectedTab)) {
    cloned.selectedTab = timewindow.selectedTab;
    if (timewindow.selectedTab === TimewindowType.REALTIME) {
      cloned.realtime = deepClone(timewindow.realtime);
    } else if (timewindow.selectedTab === TimewindowType.HISTORY) {
      cloned.history = deepClone(timewindow.history);
    }
  }
  cloned.aggregation = deepClone(timewindow.aggregation);
  cloned.timezone = timewindow.timezone;
  return cloned;
};

export interface TimeInterval {
  name: string;
  translateParams: {[key: string]: any};
  value: Interval;
}

export const defaultTimeIntervals = new Array<TimeInterval>(
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 1},
    value: SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 5},
    value: 5 * SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 10},
    value: 10 * SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 15},
    value: 15 * SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 30},
    value: 30 * SECOND
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 1},
    value: MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 2},
    value: 2 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 5},
    value: 5 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 10},
    value: 10 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 15},
    value: 15 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 30},
    value: 30 * MINUTE
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 1},
    value: HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 2},
    value: 2 * HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 5},
    value: 5 * HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 10},
    value: 10 * HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 12},
    value: 12 * HOUR
  },
  {
    name: 'timeinterval.days-interval',
    translateParams: {days: 1},
    value: DAY
  },
  {
    name: 'timeinterval.days-interval',
    translateParams: {days: 7},
    value: 7 * DAY
  },
  {
    name: 'timeinterval.type.week',
    translateParams: {},
    value: IntervalType.WEEK
  },
  {
    name: 'timeinterval.type.week-iso',
    translateParams: {},
    value: IntervalType.WEEK_ISO
  },
  {
    name: 'timeinterval.days-interval',
    translateParams: {days: 30},
    value: 30 * DAY
  },
  {
    name: 'timeinterval.type.month',
    translateParams: {},
    value: IntervalType.MONTH
  },
  {
    name: 'timeinterval.type.quarter',
    translateParams: {},
    value: IntervalType.QUARTER
  }
);

export enum TimeUnit {
  SECONDS = 'SECONDS',
  MINUTES = 'MINUTES',
  HOURS = 'HOURS',
  DAYS = 'DAYS'
}

export enum TimeUnitMilli {
  MILLISECONDS = 'MILLISECONDS'
}

export type FullTimeUnit = TimeUnit | TimeUnitMilli;

export const timeUnitTranslationMap = new Map<FullTimeUnit, string>(
  [
    [TimeUnitMilli.MILLISECONDS, 'timeunit.milliseconds'],
    [TimeUnit.SECONDS, 'timeunit.seconds'],
    [TimeUnit.MINUTES, 'timeunit.minutes'],
    [TimeUnit.HOURS, 'timeunit.hours'],
    [TimeUnit.DAYS, 'timeunit.days']
  ]
);

export interface TimezoneInfo {
  id: string;
  name: string;
  offset: string;
  nOffset: number;
  abbr: string;
}

let timezones: TimezoneInfo[] = null;
let defaultTimezone: string = null;

export const getTimezones = (): TimezoneInfo[] => {
  if (!timezones) {
    timezones = momentTz.tz.names().map((zoneName) => {
      const tz = momentTz.tz(zoneName);
      return {
        id: zoneName,
        name: zoneName.replace(/_/g, ' '),
        offset: `UTC${tz.format('Z')}`,
        nOffset: tz.utcOffset(),
        abbr: tz.zoneAbbr()
      };
    });
  }
  return timezones;
};

export const getDefaultTimezone = (): string => {
  if (!defaultTimezone) {
    defaultTimezone = momentTz.tz.guess();
  }
  return defaultTimezone;
};
export const getTimezoneInfo = (timezoneId: string, defaultTimezoneId?: string, userTimezoneByDefault?: boolean): TimezoneInfo => {
  const timezoneList = getTimezones();
  let foundTimezone = timezoneId ? timezoneList.find(timezoneInfo => timezoneInfo.id === timezoneId) : null;
  if (!foundTimezone) {
    if (userTimezoneByDefault) {
      const userTimezone = getDefaultTimezone();
      foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === userTimezone);
    } else if (defaultTimezoneId) {
      foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === defaultTimezoneId);
    }
  }
  return foundTimezone;
};

export const getDefaultTimezoneInfo = (): TimezoneInfo => {
  const userTimezone = getDefaultTimezone();
  return getTimezoneInfo(userTimezone);
};

export const getTime = (ts: number, tz?: string): moment_.Moment => {
  if (tz) {
    return moment(ts).tz(tz);
  } else {
    return moment(ts);
  }
};

export const calculateIntervalsCount = (startTs: number, timewindow: number, interval: Interval, tz?: string): number => {
  if (typeof interval === 'number') {
    return Math.ceil(timewindow / interval);
  } else {
    const current = getTime(startTs, tz);
    const endDate = getTime(startTs + timewindow, tz);
    let startInterval = startIntervalDate(current, interval);
    let endInterval = endIntervalDate(current, interval);
    let count = 0;
    while (startInterval.isBefore(endDate)) {
      count++;
      endInterval.add(1, 'milliseconds');
      startInterval = startIntervalDate(endInterval, interval);
      endInterval = endIntervalDate(endInterval, interval);
    }
    return count;
  }
};

export const startIntervalDate = (current: moment_.Moment, interval: IntervalType): moment_.Moment => {
  switch (interval) {
    case IntervalType.WEEK:
      return current.clone().startOf('week');
    case IntervalType.WEEK_ISO:
      return current.clone().startOf('isoWeek');
    case IntervalType.MONTH:
      return current.clone().startOf('month');
    case IntervalType.QUARTER:
      return current.clone().startOf('quarter');
  }
};

export const endIntervalDate = (current: moment_.Moment, interval: IntervalType): moment_.Moment => {
  switch (interval) {
    case IntervalType.WEEK:
      return current.clone().endOf('week');
    case IntervalType.WEEK_ISO:
      return current.clone().endOf('isoWeek');
    case IntervalType.MONTH:
      return current.clone().endOf('month');
    case IntervalType.QUARTER:
      return current.clone().endOf('quarter');
  }
};

export const calculateAggIntervalWithSubscriptionTimeWindow
  = (subsTw: SubscriptionTimewindow, endTs: number, timestamp: number): [number, number] =>
  calculateInterval(subsTw.startTs, endTs, subsTw.aggregation.interval, subsTw.tsOffset, subsTw.timezone, timestamp);

export const calculateAggIntervalWithWidgetTimeWindow
  = (widgetTimeWindow: WidgetTimewindow, timestamp: number): [number, number] =>
  calculateInterval(widgetTimeWindow.minTime - widgetTimeWindow.tsOffset,
    widgetTimeWindow.maxTime, widgetTimeWindow.interval, widgetTimeWindow.tsOffset, widgetTimeWindow.timezone, timestamp);

export const calculateInterval = (startTime: number, endTime: number,
                                  interval: Interval, tsOffset: number, timezone: string, timestamp: number): [number, number] => {
  let startIntervalTs: number;
  let endIntervalTs: number;
  if (typeof interval === 'number') {
    const startTs = startTime + tsOffset;
    startIntervalTs = startTs + Math.floor((timestamp - startTs) / interval) * interval;
    endIntervalTs = startIntervalTs + interval;
  } else {
    const time = getTime(timestamp, timezone);
    let startInterval = startIntervalDate(time, interval);
    const start = getTime(startTime, timezone);
    if (start.isAfter(startInterval)) {
      startInterval = start;
    }
    const endInterval = endIntervalDate(time, interval).add(1, 'milliseconds');
    startIntervalTs = startInterval.valueOf() + tsOffset;
    endIntervalTs = endInterval.valueOf() + tsOffset;
  }
  endIntervalTs = Math.min(endIntervalTs, endTime);
  return [startIntervalTs, endIntervalTs];
};

export const getCurrentTimeForComparison = (timeForComparison: moment_.unitOfTime.DurationConstructor, tz?: string): moment_.Moment =>
  getCurrentTime(tz).subtract(1, timeForComparison);

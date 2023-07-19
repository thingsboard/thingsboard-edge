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

import { Component, Inject, InjectionToken, OnInit } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import {
  TimeseriesDeleteStrategy,
  timeseriesDeleteStrategyTranslations
} from '@shared/models/telemetry/telemetry.models';
import { MINUTE } from '@shared/models/time/time.models';

export const DELETE_TIMESERIES_PANEL_DATA = new InjectionToken<any>('DeleteTimeseriesPanelData');

export interface DeleteTimeseriesPanelData {
  isMultipleDeletion: boolean;
}

@Component({
  selector: 'tb-delete-timeseries-panel',
  templateUrl: './delete-timeseries-panel.component.html',
  styleUrls: ['./delete-timeseries-panel.component.scss']
})
export class DeleteTimeseriesPanelComponent implements OnInit {

  strategy: string = TimeseriesDeleteStrategy.DELETE_ALL_DATA;

  result: string = null;

  startDateTime: Date;

  endDateTime: Date;

  rewriteLatestIfDeleted: boolean = true;

  strategiesTranslationsMap = timeseriesDeleteStrategyTranslations;

  multipleDeletionStrategies = [
    TimeseriesDeleteStrategy.DELETE_ALL_DATA,
    TimeseriesDeleteStrategy.DELETE_ALL_DATA_EXCEPT_LATEST_VALUE
  ];

  constructor(@Inject(DELETE_TIMESERIES_PANEL_DATA) public data: DeleteTimeseriesPanelData,
              public overlayRef: OverlayRef) { }

  ngOnInit(): void {
    let today = new Date();
    this.startDateTime = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
    this.endDateTime = today;
    if (this.data.isMultipleDeletion) {
      this.strategiesTranslationsMap = new Map(Array.from(this.strategiesTranslationsMap.entries())
        .filter(([strategy]) => {
          return this.multipleDeletionStrategies.includes(strategy);
      }))
    }
  }

  delete(): void {
    this.result = this.strategy;
    this.overlayRef.dispose();
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  isPeriodStrategy(): boolean {
    return this.strategy === TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD;
  }

  isDeleteLatestStrategy(): boolean {
    return this.strategy === TimeseriesDeleteStrategy.DELETE_LATEST_VALUE;
  }

  onStartDateTimeChange(newStartDateTime: Date) {
    const endDateTimeTs = this.endDateTime.getTime();
    if (newStartDateTime.getTime() >= endDateTimeTs) {
      this.startDateTime = new Date(endDateTimeTs - MINUTE);
    } else {
      this.startDateTime = newStartDateTime;
    }
  }

  onEndDateTimeChange(newEndDateTime: Date) {
    const startDateTimeTs = this.startDateTime.getTime();
    if (newEndDateTime.getTime() <= startDateTimeTs) {
      this.endDateTime = new Date(startDateTimeTs + MINUTE);
    } else {
      this.endDateTime = newEndDateTime;
    }
  }
}

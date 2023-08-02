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

import { Component, Inject, InjectionToken, OnDestroy, OnInit } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import {
  TimeseriesDeleteStrategy,
  timeseriesDeleteStrategyTranslations
} from '@shared/models/telemetry/telemetry.models';
import { MINUTE } from '@shared/models/time/time.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export const DELETE_TIMESERIES_PANEL_DATA = new InjectionToken<any>('DeleteTimeseriesPanelData');

export interface DeleteTimeseriesPanelData {
  isMultipleDeletion: boolean;
}

export interface DeleteTimeseriesPanelResult {
  strategy: TimeseriesDeleteStrategy;
  startDateTime: Date;
  endDateTime: Date;
  rewriteLatest: boolean;
}

@Component({
  selector: 'tb-delete-timeseries-panel',
  templateUrl: './delete-timeseries-panel.component.html',
  styleUrls: ['./delete-timeseries-panel.component.scss']
})
export class DeleteTimeseriesPanelComponent implements OnInit, OnDestroy {

  deleteTimeseriesFormGroup: FormGroup;

  result: DeleteTimeseriesPanelResult = null;

  strategiesTranslationsMap = timeseriesDeleteStrategyTranslations;

  multipleDeletionStrategies = [
    TimeseriesDeleteStrategy.DELETE_ALL_DATA,
    TimeseriesDeleteStrategy.DELETE_ALL_DATA_EXCEPT_LATEST_VALUE
  ];

  private destroy$ = new Subject<void>();

  constructor(@Inject(DELETE_TIMESERIES_PANEL_DATA) public data: DeleteTimeseriesPanelData,
              public overlayRef: OverlayRef,
              public fb: FormBuilder) { }

  ngOnInit(): void {
    const today = new Date();
    if (this.data.isMultipleDeletion) {
      this.strategiesTranslationsMap = new Map(Array.from(this.strategiesTranslationsMap.entries())
        .filter(([strategy]) => {
          return this.multipleDeletionStrategies.includes(strategy);
      }))
    }
    this.deleteTimeseriesFormGroup = this.fb.group({
      strategy: [TimeseriesDeleteStrategy.DELETE_ALL_DATA],
      startDateTime: [
        { value: new Date(today.getFullYear(), today.getMonth() - 1, today.getDate()), disabled: true },
        [Validators.required]
      ],
      endDateTime: [{ value: today, disabled: true }, [Validators.required]],
      rewriteLatest: [true]
    })
    this.deleteTimeseriesFormGroup.get('strategy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value === TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD) {
        this.deleteTimeseriesFormGroup.get('startDateTime').enable({onlySelf: true, emitEvent: false});
        this.deleteTimeseriesFormGroup.get('endDateTime').enable({onlySelf: true, emitEvent: false});
      } else {
        this.deleteTimeseriesFormGroup.get('startDateTime').disable({onlySelf: true, emitEvent: false});
        this.deleteTimeseriesFormGroup.get('endDateTime').disable({onlySelf: true, emitEvent: false});
      }
    })
    this.deleteTimeseriesFormGroup.get('startDateTime').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.onStartDateTimeChange(value));
    this.deleteTimeseriesFormGroup.get('endDateTime').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.onEndDateTimeChange(value));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  delete(): void {
    if (this.deleteTimeseriesFormGroup.valid) {
      this.result = this.deleteTimeseriesFormGroup.value;
      this.overlayRef.dispose();
    } else {
      this.deleteTimeseriesFormGroup.markAllAsTouched();
    }
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  isPeriodStrategy(): boolean {
    return this.deleteTimeseriesFormGroup.get('strategy').value === TimeseriesDeleteStrategy.DELETE_ALL_DATA_FOR_TIME_PERIOD;
  }

  isDeleteLatestStrategy(): boolean {
    return this.deleteTimeseriesFormGroup.get('strategy').value === TimeseriesDeleteStrategy.DELETE_LATEST_VALUE;
  }

  onStartDateTimeChange(newStartDateTime: Date) {
    if (newStartDateTime) {
      const endDateTimeTs = this.deleteTimeseriesFormGroup.get('endDateTime').value.getTime();
      if (newStartDateTime.getTime() >= endDateTimeTs) {
        this.deleteTimeseriesFormGroup.get('startDateTime')
          .patchValue(new Date(endDateTimeTs - MINUTE), {onlySelf: true, emitEvent: false});
      } else {
        this.deleteTimeseriesFormGroup.get('startDateTime')
          .patchValue(newStartDateTime, {onlySelf: true, emitEvent: false});
      }
    }
  }

  onEndDateTimeChange(newEndDateTime: Date) {
    if (newEndDateTime) {
      const startDateTimeTs = this.deleteTimeseriesFormGroup.get('startDateTime').value.getTime();
      if (newEndDateTime.getTime() <= startDateTimeTs) {
        this.deleteTimeseriesFormGroup.get('endDateTime')
          .patchValue(new Date(startDateTimeTs + MINUTE), {onlySelf: true, emitEvent: false});
      } else {
        this.deleteTimeseriesFormGroup.get('endDateTime')
          .patchValue(newEndDateTime, {onlySelf: true, emitEvent: false});
      }
    }
  }
}

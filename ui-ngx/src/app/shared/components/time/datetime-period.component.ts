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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FixedWindow } from '@shared/models/time/time.models';

@Component({
  selector: 'tb-datetime-period',
  templateUrl: './datetime-period.component.html',
  styleUrls: ['./datetime-period.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatetimePeriodComponent),
      multi: true
    }
  ]
})
export class DatetimePeriodComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  modelValue: FixedWindow;

  startDate: Date;
  endDate: Date;

  endTime: any;

  maxStartDate: Date;
  minEndDate: Date;
  maxEndDate: Date;

  changePending = false;

  private propagateChange = null;

  constructor() {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.changePending && this.propagateChange) {
      this.changePending = false;
      this.propagateChange(this.modelValue);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(datePeriod: FixedWindow): void {
    this.modelValue = datePeriod;
    if (this.modelValue) {
      this.startDate = new Date(this.modelValue.startTimeMs);
      this.endDate = new Date(this.modelValue.endTimeMs);
    } else {
      const date = new Date();
      this.startDate = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate() - 1,
        date.getHours(),
        date.getMinutes(),
        date.getSeconds(),
        date.getMilliseconds());
      this.endDate = date;
      this.updateView();
    }
    this.updateMinMaxDates();
  }

  updateView() {
    let value: FixedWindow = null;
    if (this.startDate && this.endDate) {
      value = {
        startTimeMs: this.startDate.getTime(),
        endTimeMs: this.endDate.getTime()
      };
    }
    this.modelValue = value;
    if (!this.propagateChange) {
      this.changePending = true;
    } else {
      this.propagateChange(this.modelValue);
    }
  }

  updateMinMaxDates() {
    this.maxStartDate = new Date(this.endDate.getTime() - 1000);
    this.minEndDate = new Date(this.startDate.getTime() + 1000);
    this.maxEndDate = new Date();
  }

  onStartDateChange() {
    if (this.startDate) {
      if (this.startDate.getTime() > this.maxStartDate.getTime()) {
        this.startDate = new Date(this.maxStartDate.getTime());
      }
      this.updateMinMaxDates();
    }
    this.updateView();
  }

  onEndDateChange() {
    if (this.endDate) {
      if (this.endDate.getTime() < this.minEndDate.getTime()) {
        this.endDate = new Date(this.minEndDate.getTime());
      } else if (this.endDate.getTime() > this.maxEndDate.getTime()) {
        this.endDate = new Date(this.maxEndDate.getTime());
      }
      this.updateMinMaxDates();
    }
    this.updateView();
  }

}

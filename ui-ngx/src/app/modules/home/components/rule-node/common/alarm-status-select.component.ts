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

import { Component, DestroyRef, forwardRef, OnInit } from '@angular/core';
import { AlarmStatus, alarmStatusTranslations, PageComponent } from '@shared/public-api';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-alarm-status-select',
  templateUrl: './alarm-status-select.component.html',
  styleUrls: ['./alarm-status-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AlarmStatusSelectComponent),
    multi: true
  }]
})

export class AlarmStatusSelectComponent extends PageComponent implements OnInit, ControlValueAccessor {

  public alarmStatusGroup: FormGroup;

  private propagateChange = null;

  readonly alarmStatus = AlarmStatus;
  readonly alarmStatusTranslations = alarmStatusTranslations;

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
    super();
  }

  ngOnInit(): void {
    this.alarmStatusGroup = this.fb.group({
      alarmStatus: [null, []]
    });

    this.alarmStatusGroup.get('alarmStatus').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      this.propagateChange(value);
    });
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.alarmStatusGroup.disable({emitEvent: false});
    } else {
      this.alarmStatusGroup.enable({emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  writeValue(value: Array<AlarmStatus>): void {
    this.alarmStatusGroup.get('alarmStatus').patchValue(value, {emitEvent: false});
  }
}

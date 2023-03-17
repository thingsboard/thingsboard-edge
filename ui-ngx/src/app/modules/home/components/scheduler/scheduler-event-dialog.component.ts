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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { SchedulerEvent } from '@shared/models/scheduler-event.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';
import { deepClone, isObject, isString } from '@core/utils';

export interface SchedulerEventDialogData {
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};
  isAdd: boolean;
  readonly: boolean;
  schedulerEvent: SchedulerEvent;
  defaultEventType: string;
}

@Component({
  selector: 'tb-scheduler-event-dialog',
  templateUrl: './scheduler-event-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: SchedulerEventDialogComponent}],
  styleUrls: ['./scheduler-event-dialog.component.scss']
})
export class SchedulerEventDialogComponent extends DialogComponent<SchedulerEventDialogComponent, boolean>
  implements OnInit, ErrorStateMatcher {

  schedulerEventFormGroup: UntypedFormGroup;

  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};
  isAdd: boolean;
  readonly: boolean;
  schedulerEvent: SchedulerEvent;
  defaultEventType: string;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SchedulerEventDialogData,
              private schedulerEventService: SchedulerEventService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<SchedulerEventDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.schedulerEventConfigTypes = data.schedulerEventConfigTypes;
    this.isAdd = data.isAdd;
    this.readonly = data.readonly;
    this.schedulerEvent = data.schedulerEvent;
    this.defaultEventType = data.defaultEventType;
  }

  ngOnInit(): void {
    const configuration = deepClone(this.schedulerEvent.configuration);
    if (configuration && this.schedulerEvent.originatorId) {
      configuration.originatorId = this.schedulerEvent.originatorId;
    }
    this.schedulerEventFormGroup = this.fb.group({
      name: [this.schedulerEvent.name, [Validators.required, Validators.maxLength(255)]],
      type: [this.isAdd ? this.defaultEventType : this.schedulerEvent.type, [Validators.required]],
      configuration: [configuration, [Validators.required]],
      schedule: [this.schedulerEvent.schedule, [Validators.required]]
    });
    if (this.readonly) {
      this.schedulerEventFormGroup.disable();
    } else if (this.defaultEventType) {
      this.schedulerEventFormGroup.get('type').disable();
    } else if (!this.readonly) {
      this.schedulerEventFormGroup.get('type').valueChanges.subscribe((newVal) => {
        const prevVal = this.schedulerEventFormGroup.value.type;
        if (newVal !== prevVal && newVal) {
          this.schedulerEventFormGroup.get('configuration').patchValue({
            originatorId: null,
            msgType: null,
            msgBody: {},
            metadata: {}
          }, {emitEvent: false});
        }
      });
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.submitted = true;
    if (!this.schedulerEventFormGroup.invalid) {
      const schedulerEventValue = this.schedulerEventFormGroup.getRawValue();
      this.schedulerEvent.originatorId = schedulerEventValue.configuration?.originatorId;
      if (schedulerEventValue.configuration?.originatorId) {
        delete schedulerEventValue.configuration?.originatorId;
      }
      this.schedulerEvent = {...this.schedulerEvent, ...schedulerEventValue};
      this.schedulerEventService.saveSchedulerEvent(this.deepTrim(this.schedulerEvent)).subscribe(
        () => {
            this.dialogRef.close(true);
        }
      );
    }
  }

  private deepTrim<T>(obj: T): T {
    return Object.keys(obj).reduce((acc, curr) => {
      if (isString(obj[curr])) {
        acc[curr] = obj[curr].trim();
      } else if (isObject(obj[curr])) {
        acc[curr] = this.deepTrim(obj[curr]);
      } else {
        acc[curr] = obj[curr];
      }
      return acc;
    }, Array.isArray(obj) ? [] : {}) as T;
  }
}

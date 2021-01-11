///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { SchedulerEvent } from '@shared/models/scheduler-event.models';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';
import { isObject, isString } from '@core/utils';

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

  schedulerEventFormGroup: FormGroup;

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
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.schedulerEventConfigTypes = data.schedulerEventConfigTypes;
    this.isAdd = data.isAdd;
    this.readonly = data.readonly;
    this.schedulerEvent = data.schedulerEvent;
    this.defaultEventType = data.defaultEventType;
  }

  ngOnInit(): void {
    this.schedulerEventFormGroup = this.fb.group({
      name: [this.schedulerEvent.name, [Validators.required]],
      type: [this.isAdd ? this.defaultEventType : this.schedulerEvent.type, [Validators.required]],
      configuration: [this.schedulerEvent.configuration, [Validators.required]],
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

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
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
      this.schedulerEvent = {...this.schedulerEvent, ...this.schedulerEventFormGroup.getRawValue()};
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

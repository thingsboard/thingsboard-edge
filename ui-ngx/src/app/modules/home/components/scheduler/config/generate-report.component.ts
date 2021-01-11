///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';

@Component({
  selector: 'tb-generate-report-event-config',
  templateUrl: './generate-report.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => GenerateReportComponent),
    multi: true
  }]
})
export class GenerateReportComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: SchedulerEventConfiguration | null;

  generateReportFormGroup: FormGroup;

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.generateReportFormGroup = this.fb.group({
      msgBody: this.fb.group(
        {
          reportConfig: [null, [Validators.required]],
          sendEmail: [false, []],
          emailConfig: [null, [Validators.required]]
        }
      )
    });

    this.generateReportFormGroup.get('msgBody.sendEmail').valueChanges.subscribe(() => {
      this.updateEnabledState();
    });

    this.generateReportFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.generateReportFormGroup.disable({emitEvent: false});
    } else {
      this.generateReportFormGroup.enable({emitEvent: false});
      const sendEmail: boolean = this.generateReportFormGroup.get('msgBody.sendEmail').value;
      if (sendEmail) {
        this.generateReportFormGroup.get('msgBody.emailConfig').enable({emitEvent: false});
      } else {
        this.generateReportFormGroup.get('msgBody.emailConfig').disable({emitEvent: false});
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = value;
    this.generateReportFormGroup.reset(this.modelValue || undefined,{emitEvent: false});
    this.updateEnabledState();
  }

  private updateModel() {
    if (this.generateReportFormGroup.valid) {
      const value = this.generateReportFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}

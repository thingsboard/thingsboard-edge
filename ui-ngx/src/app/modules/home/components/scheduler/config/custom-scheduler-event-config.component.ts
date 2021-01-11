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

import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AfterViewInit, Directive, DoCheck, Inject, OnInit, QueryList, ViewChildren } from '@angular/core';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { deepClone, isEqual } from '@core/utils';
import { ControlValueAccessor, NgForm } from '@angular/forms';

@Directive()
export class CustomSchedulerEventConfigComponent extends PageComponent implements OnInit, AfterViewInit, DoCheck, ControlValueAccessor {

  @ViewChildren(NgForm, {read: NgForm}) forms: QueryList<NgForm>;

  configuration: SchedulerEventConfiguration;
  private configurationSnapshot: SchedulerEventConfiguration;

  disabled: boolean;

  [key: string]: any;

  private propagateChange = (v: any) => { };

  constructor(@Inject(Store) protected store: Store<AppState>) {
    super(store);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit(): void {
    this.configurationSnapshot = deepClone(this.configuration);
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateFormsDisabledState();
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.configurationSnapshot = deepClone(value);
    this.configuration = value;
  }

  ngAfterViewInit() {
    if (this.forms) {
      this.updateFormsDisabledState();
      this.forms.changes.subscribe(() => {
        this.updateFormsDisabledState();
      });
    }
  }

  ngDoCheck(): void {
    const newConfiguration = this.validate() ? this.configuration : null;
    if (!isEqual(this.configurationSnapshot, newConfiguration)) {
      this.configurationSnapshot = deepClone(newConfiguration);
      setTimeout(() => {
        this.propagateChange(newConfiguration);
      }, 0);
    }
  }

  private updateFormsDisabledState() {
    if (this.forms) {
      setTimeout(() => {
        this.forms.toArray().forEach((form) => {
          if (this.disabled) {
            form.control.disable();
          } else {
            form.control.enable();
          }
        })
      }, 0);
    }
  }

  private validate(): boolean {
    if (this.forms) {
      const res = this.forms.toArray().filter((form) => form.valid === false);
      if (res && res.length) {
        return false;
      }
    }
    return true;
  }
}

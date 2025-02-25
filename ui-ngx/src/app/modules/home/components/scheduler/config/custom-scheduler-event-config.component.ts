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

import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, Directive, DoCheck, OnInit, QueryList, ViewChildren } from '@angular/core';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { deepClone, isEqual } from '@core/utils';
import { AbstractControl, ControlValueAccessor, NgForm, ValidationErrors, Validator } from '@angular/forms';

@Directive()
export class CustomSchedulerEventConfigComponent
  extends PageComponent implements OnInit, AfterViewInit, DoCheck, ControlValueAccessor, Validator {

  @ViewChildren(NgForm, {read: NgForm}) forms: QueryList<NgForm>;

  configuration: SchedulerEventConfiguration;
  private configurationSnapshot: SchedulerEventConfiguration;

  disabled: boolean;

  [key: string]: any;

  private propagateChange = (_v: any) => { };

  constructor() {
    super();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
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
    const newConfiguration = this.doValidate() ? this.configuration : null;
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
        });
      }, 0);
    }
  }

  public validate(_control: AbstractControl): ValidationErrors | null {
    if (!this.doValidate()) {
      return {
        customSchedulerEventForm: {
          valid: false
        }
      };
    } else {
      return null;
    }
  }

  private doValidate(): boolean {
    if (this.forms) {
      const res = this.forms.toArray().filter((form) => form.valid === false);
      if (res && res.length) {
        return false;
      }
    }
    return true;
  }
}

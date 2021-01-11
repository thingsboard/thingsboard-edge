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
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { PageComponent } from '@shared/components/page.component';

interface EmailConfig {
  from: string;
  to: string;
  cc?: string;
  bcc?: string;
  subject: string;
  body: string;
}

@Component({
  selector: 'tb-email-config',
  templateUrl: './email-config.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EmailConfigComponent),
    multi: true
  }]
})
export class EmailConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: EmailConfig | null;

  emailConfigFormGroup: FormGroup;

  @Input()
  disabled: boolean;

  authUser = getCurrentAuthUser(this.store);

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.emailConfigFormGroup = this.fb.group({
      from: [null, [Validators.required]],
      to: [null, [Validators.required]],
      cc: [null, []],
      bcc: [null, []],
      subject: [null, [Validators.required]],
      body: [null, [Validators.required]]
    });

    this.emailConfigFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
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
    if (this.disabled) {
      this.emailConfigFormGroup.disable({emitEvent: false});
    } else {
      this.emailConfigFormGroup.enable({emitEvent: false});
    }
    this.checkModel();
  }

  private checkModel() {
    if (!this.disabled && !this.modelValue) {
      this.modelValue = this.createDefaultEmailConfig();
      this.emailConfigFormGroup.reset(this.modelValue,{emitEvent: false});
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  writeValue(value: EmailConfig | null): void {
    this.modelValue = value;
    this.emailConfigFormGroup.reset(this.modelValue || undefined,{emitEvent: false});
  }

  private createDefaultEmailConfig(): EmailConfig {
    const emailConfig: EmailConfig = {
      from: this.authUser.sub,
      to: null,
      subject: 'Report generated on %d{yyyy-MM-dd HH:mm:ss}',
      body: 'Report was successfully generated on %d{yyyy-MM-dd HH:mm:ss}.\nSee attached report file.'
    };
    return emailConfig;
  }

  private updateModel() {
    if (this.emailConfigFormGroup.valid) {
      const value = this.emailConfigFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}

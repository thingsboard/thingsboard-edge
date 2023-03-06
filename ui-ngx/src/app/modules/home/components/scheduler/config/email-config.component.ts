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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
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

  emailConfigFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  authUser = getCurrentAuthUser(this.store);

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
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

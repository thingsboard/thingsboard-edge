///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { booleanAttribute, Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  CustomMobilePage,
  MobilePageType,
  mobilePageTypeTranslations,
  WEB_URL_REGEX
} from '@shared/models/mobile-app.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-mobile-page-item',
  templateUrl: './custom-mobile-page.component.html',
  styleUrls: ['./custom-mobile-page.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomMobilePageComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CustomMobilePageComponent),
      multi: true
    }
  ]
})
export class CustomMobilePageComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  mobilePagesTypes = [MobilePageType.DASHBOARD, MobilePageType.WEB_VIEW, MobilePageType.CUSTOM];
  MobilePageType = MobilePageType;
  mobilePageTypeTranslations = mobilePageTypeTranslations;

  customMobilePageForm = this.fb.group({
    visible: [true],
    icon: ['star'],
    label: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]],
    type: [MobilePageType.DASHBOARD],
    dashboardId: this.fb.control<string>(null, Validators.required),
    url: [{value:'', disabled: true}, [Validators.required, Validators.pattern(WEB_URL_REGEX)]],
    path: [{value:'', disabled: true}, [Validators.required, Validators.pattern(/^(\/[\w\-._~:/?#[\]@!$&'()*+,;=%]*)?$/)]]
  });

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private store: Store<AppState>) {
    this.customMobilePageForm.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(type => {
      this.customMobilePageForm.get('dashboardId').disable({emitEvent: false});
      this.customMobilePageForm.get('url').disable({emitEvent: false});
      this.customMobilePageForm.get('path').disable({emitEvent: false});
      switch (type) {
        case MobilePageType.DASHBOARD:
          this.customMobilePageForm.get('dashboardId').enable({emitEvent: false});
          break;
        case MobilePageType.WEB_VIEW:
          this.customMobilePageForm.get('url').enable({emitEvent: false});
          break;
        case MobilePageType.CUSTOM:
          this.customMobilePageForm.get('path').enable({emitEvent: false});
          break;
      }
    });

    if (getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN) {
      this.mobilePagesTypes.shift();
      this.customMobilePageForm.get('type').setValue(MobilePageType.WEB_VIEW);
    }

    this.customMobilePageForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => this.propagateChange(value))
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.customMobilePageForm.disable({emitEvent: false});
    } else {
      this.customMobilePageForm.enable({emitEvent: false});
      this.customMobilePageForm.get('type').updateValueAndValidity({onlySelf: true});
    }
  }

  validate(): ValidationErrors | null {
    if (!this.customMobilePageForm.valid) {
      return {
        invalidCustomMobilePageForm: true
      };
    }
    return null;
  }

  writeValue(value: CustomMobilePage) {
    this.customMobilePageForm.patchValue(value, {emitEvent: false});
  }
}

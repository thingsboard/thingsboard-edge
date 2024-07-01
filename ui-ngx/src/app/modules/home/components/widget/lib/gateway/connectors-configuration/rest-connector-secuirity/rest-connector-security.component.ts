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

import {
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  OnDestroy,
} from '@angular/core';
import { Subject } from 'rxjs';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntil } from 'rxjs/operators';
import {
  noLeadTrailSpacesRegex,
  RestSecurityType,
  RestSecurityTypeTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tb-rest-connector-security',
  templateUrl: './rest-connector-security.component.html',
  styleUrls: ['./rest-connector-security.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RestConnectorSecurityComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RestConnectorSecurityComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
  ]
})
export class RestConnectorSecurityComponent implements ControlValueAccessor, Validator, OnDestroy {
  BrokerSecurityType = RestSecurityType;
  securityTypes: RestSecurityType[] = Object.values(RestSecurityType);
  SecurityTypeTranslationsMap = RestSecurityTypeTranslationsMap;
  securityFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder) {
    this.securityFormGroup = this.fb.group({
      type: [RestSecurityType.ANONYMOUS, []],
      username: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      password: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
    });
    this.observeSecurityForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfo: any): void {
    if (!deviceInfo.type) {
      deviceInfo.type = RestSecurityType.ANONYMOUS;
    }
    this.securityFormGroup.reset(deviceInfo);
    this.updateView(deviceInfo);
  }

  validate(): ValidationErrors | null {
    return this.securityFormGroup.valid ? null : {
      securityForm: { valid: false }
    };
  }

  private updateView(value: any): void {
    this.propagateChange(value);
  }

  private updateValidators(type: RestSecurityType): void {
    if (type === RestSecurityType.BASIC) {
      this.securityFormGroup.get('username').enable({emitEvent: false});
      this.securityFormGroup.get('password').enable({emitEvent: false});
    } else {
      this.securityFormGroup.get('username').disable({emitEvent: false});
      this.securityFormGroup.get('password').disable({emitEvent: false});
    }
  }

  private observeSecurityForm(): void {
    this.securityFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => this.updateView(value));

    this.securityFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => this.updateValidators(type));
  }
}

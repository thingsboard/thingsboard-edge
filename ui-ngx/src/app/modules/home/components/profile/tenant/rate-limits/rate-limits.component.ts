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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import {
  RateLimitsDetailsDialogComponent,
  RateLimitsDetailsDialogData
} from '@home/components/profile/tenant/rate-limits/rate-limits-details-dialog.component';
import {
  RateLimits,
  rateLimitsDialogTitleTranslationMap,
  rateLimitsLabelTranslationMap,
  RateLimitsType,
  stringToRateLimitsArray
} from './rate-limits.models';
import { isDefined } from '@core/utils';

@Component({
  selector: 'tb-rate-limits',
  templateUrl: './rate-limits.component.html',
  styleUrls: ['./rate-limits.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RateLimitsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RateLimitsComponent),
      multi: true,
    }
  ]
})
export class RateLimitsComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  type: RateLimitsType;

  label: string;

  rateLimitsFormGroup: FormGroup;

  get rateLimitsArray(): Array<RateLimits> {
    return this.rateLimitsFormGroup.get('rateLimits').value;
  }

  private modelValue: string;

  private propagateChange = null;

  constructor(private dialog: MatDialog,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.label = rateLimitsLabelTranslationMap.get(this.type);
    this.rateLimitsFormGroup = this.fb.group({
      rateLimits: [null, []]
    });
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.rateLimitsFormGroup.disable({emitEvent: false});
    } else {
      this.rateLimitsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string) {
    this.modelValue = value;
    this.updateRateLimitsInfo();
  }

  public validate(c: FormControl) {
    return null;
  }

  public onClick($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = rateLimitsDialogTitleTranslationMap.get(this.type);
    this.dialog.open<RateLimitsDetailsDialogComponent, RateLimitsDetailsDialogData,
      string>(RateLimitsDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        rateLimits: this.modelValue,
        title,
        readonly: this.disabled
      }
    }).afterClosed().subscribe((result) => {
      if (isDefined(result)) {
        this.modelValue = result;
        this.updateModel();
      }
    });
  }

  private updateRateLimitsInfo() {
    this.rateLimitsFormGroup.patchValue(
      {
        rateLimits: stringToRateLimitsArray(this.modelValue)
      }
    );
  }

  private updateModel() {
    this.updateRateLimitsInfo();
    this.propagateChange(this.modelValue);
  }

}

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

import { Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import {
  MappingDataKey,
  MappingValueType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-type-value-panel',
  templateUrl: './type-value-panel.component.html',
  styleUrls: ['./type-value-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TypeValuePanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TypeValuePanelComponent),
      multi: true
    }
  ]
})
export class TypeValuePanelComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  valueTypeKeys: MappingValueType[] = Object.values(MappingValueType);
  valueTypes = mappingValueTypesMap;
  valueListFormArray: UntypedFormArray;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.valueListFormArray = this.fb.array([]);
    this.valueListFormArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByKey(_: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      type: [MappingValueType.STRING, []],
      value: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    this.valueListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.valueListFormArray.removeAt(index);
    this.valueListFormArray.markAsDirty();
  }

  valueTitle(value: any): string {
    if (isDefinedAndNotNull(value)) {
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return value;
    }
    return '';
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfoArray: Array<MappingDataKey>): void {
    for (const deviceInfo of deviceInfoArray) {
      const dataKeyFormGroup = this.fb.group({
        type: [deviceInfo.type, []],
        value: [deviceInfo.value, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
      });
      this.valueListFormArray.push(dataKeyFormGroup);
    }
  }

  validate(): ValidationErrors | null {
    return this.valueListFormArray.valid ? null : {
      valueListForm: { valid: false }
    };
  }

  private updateView(value: any): void {
    this.propagateChange(value);
  }
}

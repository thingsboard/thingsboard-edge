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

import { Component, EventEmitter, forwardRef, Output } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { MapActionButtonSettings } from '@shared/models/widget/maps/map.models';
import { WidgetAction, WidgetActionType, widgetType } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-map-action-button-row',
  templateUrl: 'map-action-button-row.component.html',
  providers: [{
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapActionButtonRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapActionButtonRowComponent),
      multi: true
    }]
})
export class MapActionButtonRowComponent implements ControlValueAccessor, Validator {

  @Output()
  buttonRemoved = new EventEmitter();

  mapActionButton = this.fb.group({
    label: [''],
    icon: [''],
    color: [''],
    action: this.fb.control<WidgetAction>(null)
  }, {validators: this.validateButtonConfig()});

  additionalWidgetActionTypes = [WidgetActionType.placeMapItem];
  readonly widgetType = widgetType;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.mapActionButton.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.propagateChange(value))
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void { }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.mapActionButton.disable({emitEvent: false});
    } else {
      this.mapActionButton.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.mapActionButton.valid ? null : {
      mapButtonAction: false
    };
  }

  writeValue(value: MapActionButtonSettings) {
   this.mapActionButton.patchValue(value, {emitEvent: false});
  }

  private validateButtonConfig() {
    return (c: FormGroup) => {
      return !c.value.icon && isEmptyStr(c.value.label)  ? {
        invalidButtonConfig: true
      } : null;
    };
  }
}

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

import { Component, forwardRef } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {
  defaultMapActionButtonSettings,
  MapActionButtonSettings
} from '@shared/models/widget/maps/map.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-map-action-button-settings',
  templateUrl: './map-action-buttons-settings.component.html',
  providers: [{
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapActionButtonsSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapActionButtonsSettingsComponent),
      multi: true
    }]
})
export class MapActionButtonsSettingsComponent implements ControlValueAccessor, Validator {

  mapActionButtonsForm = this.fb.array<MapActionButtonSettings>([]);

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.mapActionButtonsForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.propagateChange(value));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void { }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.mapActionButtonsForm.disable({emitEvent: false});
    } else {
      this.mapActionButtonsForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.mapActionButtonsForm.valid ? null : {
      mapActionButtons: false
    };
  }

  writeValue(buttons: MapActionButtonSettings[] = []) {
    if (buttons?.length === this.mapActionButtonsForm.length) {
      this.mapActionButtonsForm.patchValue(buttons, {emitEvent: false});
    } else {
      this.mapActionButtonsForm.clear({emitEvent: false});
      buttons.forEach(
        button => this.mapActionButtonsForm.push(this.fb.control(button), {emitEvent: false})
      );
    }
  }

  get dragEnabled(): boolean {
    return this.mapActionButtonsForm.length > 1;
  }

  buttonDrop(event: CdkDragDrop<string[]>) {
    const actionButton = this.mapActionButtonsForm.at(event.previousIndex);
    this.mapActionButtonsForm.removeAt(event.previousIndex, {emitEvent: false});
    this.mapActionButtonsForm.insert(event.currentIndex, actionButton);
  }

  addButton() {
    this.mapActionButtonsForm.push(this.fb.control(defaultMapActionButtonSettings));
  }

  removeButton(index: number) {
    this.mapActionButtonsForm.removeAt(index);
  }
}

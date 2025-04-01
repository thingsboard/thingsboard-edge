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

import { Component, forwardRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MapItemTooltips, MapItemType, mapItemTooltipsTranslation } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepTrim, isEqual } from '@core/utils';

@Component({
  selector: 'tb-map-item-tooltips',
  templateUrl: './map-item-tooltips.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapItemTooltipsComponent),
      multi: true
    }
  ]
})
export class MapItemTooltipsComponent implements ControlValueAccessor, OnChanges {

  @Input({required: true})
  mapItemType: MapItemType;

  tooltipsForm: FormGroup;
  MapItemType = MapItemType;
  readonly mapItemTooltipsDefaultTranslate = mapItemTooltipsTranslation;

  private modelValue: MapItemTooltips;
  private propagateChange = (_val: any) => {};

  constructor(private fd: FormBuilder) {
    this.tooltipsForm = this.fd.group({
      placeMarker: [''],
      firstVertex: [''],
      continueLine: [''],
      finishPoly: [''],
      startRect: [''],
      finishRect: [''],
      startCircle: [''],
      finishCircle: ['']
    });

    this.tooltipsForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(this.updatedModel.bind(this));
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.mapItemType) {
      const mapItemTypeChanges = changes.mapItemType;
      if (!mapItemTypeChanges.firstChange && mapItemTypeChanges.currentValue !== mapItemTypeChanges.previousValue) {
        this.updatedValidators(true);
      }
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.tooltipsForm.disable({emitEvent: false});
    } else {
      this.tooltipsForm.enable({emitEvent: false});
    }
  }

  writeValue(obj: MapItemTooltips) {
    this.modelValue = obj;
    this.tooltipsForm.patchValue(obj, {emitEvent: false});
    this.updatedValidators();
  }

  private updatedValidators(emitNewValue = false) {
    this.tooltipsForm.disable({emitEvent: false});
    switch (this.mapItemType) {
      case MapItemType.marker:
        this.tooltipsForm.get('placeMarker').enable({emitEvent: false});
        break;
      case MapItemType.rectangle:
        this.tooltipsForm.get('startRect').enable({emitEvent: false});
        this.tooltipsForm.get('finishRect').enable({emitEvent: false});
        break;
      case MapItemType.polygon:
        this.tooltipsForm.get('firstVertex').enable({emitEvent: false});
        this.tooltipsForm.get('continueLine').enable({emitEvent: false});
        this.tooltipsForm.get('finishPoly').enable({emitEvent: false});
        break;
      case MapItemType.circle:
        this.tooltipsForm.get('startCircle').enable({emitEvent: false});
        this.tooltipsForm.get('finishCircle').enable({emitEvent: false});
        break;
    }
    this.tooltipsForm.updateValueAndValidity({emitEvent: emitNewValue})
  }

  private updatedModel(value: MapItemTooltips) {
    const currentValue = Object.fromEntries(Object.entries(deepTrim(value)).filter(([_, v]) => v != ''));
    if (!isEqual(currentValue, this.modelValue)) {
      this.modelValue = currentValue;
      this.propagateChange(currentValue);
    }
  }
}

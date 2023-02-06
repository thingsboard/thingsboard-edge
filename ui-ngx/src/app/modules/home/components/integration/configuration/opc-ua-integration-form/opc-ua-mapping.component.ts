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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { OpcMappingType, OpcMappingTypeTranslation, OpcUaMapping } from '@shared/models/integration.models';

@Component({
  selector: 'tb-opc-ua-mapping',
  templateUrl: './opc-ua-mapping.component.html',
  styleUrls: ['opc-ua-mapping.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OpcUaMappingComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => OpcUaMappingComponent),
    multi: true,
  }]
})
export class OpcUaMappingComponent implements ControlValueAccessor, Validator, OnDestroy {

  opcMappingForm: UntypedFormGroup;

  OpcMappingTypes = Object.values(OpcMappingType) as Array<OpcMappingType>;
  OpcMappingType = OpcMappingType;
  OpcMappingTypeTranslation = OpcMappingTypeTranslation;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.opcMappingForm = this.fb.group({
      map: this.fb.array([], Validators.required)
    });
    this.opcMappingForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.updateModels(value.map);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.opcMappingForm.disable({emitEvent: false});
    } else {
      this.opcMappingForm.enable({emitEvent: false});
    }
  }

  writeValue(mappings: OpcUaMapping[]): void {
    if (isDefinedAndNotNull(mappings)) {
      if (this.mapFormArray.length === mappings.length) {
        this.opcMappingForm.get('map').patchValue(mappings, {emitEvent: false});
      } else {
        const mapControls: Array<UntypedFormGroup> = [];
        mappings.forEach((map) => {
          mapControls.push(this.createdFormGroup(map));
        });
        this.opcMappingForm.setControl('map', this.fb.array(mapControls), {emitEvent: false});
        if (this.disabled) {
          this.opcMappingForm.disable({emitEvent: false});
        }
      }
    } else {
      this.addMap();
    }
  }

  validate(): ValidationErrors | null {
    return this.opcMappingForm.valid && this.mapFormArray.length ? null : {
      opcMappingForm: {valid: false}
    };
  }

  addMap() {
    this.mapFormArray.push(this.createdFormGroup());
  }

  removeMap(index: number) {
    this.mapFormArray.removeAt(index);
  }

  get mapFormArray(): UntypedFormArray {
    return this.opcMappingForm.get('map') as UntypedFormArray;
  }

  get mapFormArrayControls(): UntypedFormGroup[] {
    return this.mapFormArray.controls as UntypedFormGroup[];
  }

  private createdFormGroup(value?): UntypedFormGroup {
    return this.fb.group({
      deviceNodePattern: [value?.deviceNodePattern || 'Channel1\\.Device\\d+$', Validators.required],
      mappingType: [value?.mappingType || OpcMappingType.FQN, Validators.required],
      subscriptionTags: [value?.subscriptionTags || null, Validators.required],
      namespace: [value?.namespace || null, Validators.min(0)]
    });
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

}

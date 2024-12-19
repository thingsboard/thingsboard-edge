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
  Component,
  DestroyRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  QueryList,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { ScadaSymbolProperty, ScadaSymbolPropertyType } from '@home/components/widget/lib/scada/scada-symbol.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  propertyValid,
  ScadaSymbolPropertyRowComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-property-row.component';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-scada-symbol-metadata-properties',
  templateUrl: './scada-symbol-properties.component.html',
  styleUrls: ['./scada-symbol-properties.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolPropertiesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolPropertiesComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolPropertiesComponent implements ControlValueAccessor, OnInit, Validator {

  @HostBinding('style.display') styleDisplay = 'flex';
  @HostBinding('style.overflow') styleOverflow = 'hidden';

  @ViewChildren(ScadaSymbolPropertyRowComponent)
  propertyRows: QueryList<ScadaSymbolPropertyRowComponent>;

  @Input()
  disabled: boolean;

  booleanPropertyIds: string[] = [];

  propertiesFormGroup: UntypedFormGroup;

  errorText = '';

  get dragEnabled(): boolean {
    return !this.disabled && this.propertiesFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.propertiesFormGroup = this.fb.group({
      properties: this.fb.array([])
    });
    this.propertiesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let properties: ScadaSymbolProperty[] = this.propertiesFormGroup.get('properties').value;
        if (properties) {
          properties = properties.filter(p => propertyValid(p));
        }
        this.booleanPropertyIds = properties.filter(p => p.type === ScadaSymbolPropertyType.switch).map(p => p.id);
        properties.forEach((p, i) => {
          if (p.disableOnProperty && !this.booleanPropertyIds.includes(p.disableOnProperty)) {
            p.disableOnProperty = null;
            const controls = this.propertiesFormArray().controls;
            controls[i].patchValue(p, {emitEvent: false});
          }
        });
        this.propagateChange(properties);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.propertiesFormGroup.disable({emitEvent: false});
    } else {
      this.propertiesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolProperty[] | undefined): void {
    const properties= value || [];
    this.propertiesFormGroup.setControl('properties', this.preparePropertiesFormArray(properties), {emitEvent: false});
    this.booleanPropertyIds = properties.filter(p => p.type === ScadaSymbolPropertyType.switch).map(p => p.id);
  }

  public validate(c: UntypedFormControl) {
    this.errorText = '';
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const notUniqueControls =
      propertiesArray.controls.filter(control => control.hasError('propertyIdNotUnique'));
    for (const control of notUniqueControls) {
      control.updateValueAndValidity({onlySelf: false, emitEvent: false});
      if (control.hasError('propertyIdNotUnique')) {
        this.errorText = this.translate.instant('scada.property.not-unique-property-ids-error');
      }
    }
    const valid =  this.propertiesFormGroup.valid;
    return valid ? null : {
      properties: {
        valid: false,
      },
    };
  }

  public propertyIdUnique(id: string, index: number): boolean {
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    for (let i = 0; i < propertiesArray.controls.length; i++) {
      if (i !== index) {
        const otherControl = propertiesArray.controls[i];
        if (id === otherControl.value.id) {
          return false;
        }
      }
    }
    return true;
  }

  propertyDrop(event: CdkDragDrop<string[]>) {
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const property = propertiesArray.at(event.previousIndex);
    propertiesArray.removeAt(event.previousIndex);
    propertiesArray.insert(event.currentIndex, property);
  }

  propertiesFormArray(): UntypedFormArray {
    return this.propertiesFormGroup.get('properties') as UntypedFormArray;
  }

  trackByProperty(index: number, propertyControl: AbstractControl): any {
    return propertyControl;
  }

  removeProperty(index: number, emitEvent = true) {
    (this.propertiesFormGroup.get('properties') as UntypedFormArray).removeAt(index, {emitEvent});
  }

  addProperty() {
    const property: ScadaSymbolProperty = {
      id: '',
      name: '',
      type: ScadaSymbolPropertyType.text,
      default: ''
    };
    const propertiesArray = this.propertiesFormGroup.get('properties') as UntypedFormArray;
    const propertyControl = this.fb.control(property, []);
    propertiesArray.push(propertyControl);
    setTimeout(() => {
      const propertyRow = this.propertyRows.get(this.propertyRows.length-1);
      propertyRow.onAdd(() => {
        this.removeProperty(propertiesArray.length-1);
      });
    });
  }

  private preparePropertiesFormArray(properties: ScadaSymbolProperty[] | undefined): UntypedFormArray {
    const propertiesControls: Array<AbstractControl> = [];
    if (properties) {
      properties.forEach((property) => {
        propertiesControls.push(this.fb.control(property, []));
      });
    }
    return this.fb.array(propertiesControls);
  }
}

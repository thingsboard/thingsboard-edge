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
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Instance, ResourceLwM2M, ResourceSettingTelemetry, } from './lwm2m-profile-config.models';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-instances',
  templateUrl: './lwm2m-observe-attr-telemetry-instances.component.html',
  styleUrls: [ './lwm2m-observe-attr-telemetry-instances.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryInstancesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryInstancesComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryInstancesComponent implements ControlValueAccessor, Validator, OnDestroy {

  instancesFormGroup: FormGroup;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  private valueChange$: Subscription = null;
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              public translate: TranslateService) {
    this.instancesFormGroup = this.fb.group({
      instances: this.fb.array([])
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.instancesFormGroup.disable({emitEvent: false});
    } else {
      this.instancesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Instance[]): void {
    this.updateInstances(value);
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return this.instancesFormGroup.valid ? null : {
      instancesForm: false
    };
  }

  get instancesFormArray(): FormArray {
    return this.instancesFormGroup.get('instances') as FormArray;
  }

  private updateInstances(instances: Instance[]): void {
    if (instances.length === this.instancesFormArray.length) {
      this.instancesFormArray.patchValue(instances, {emitEvent: false});
    } else {
      if (this.valueChange$) {
        this.valueChange$.unsubscribe();
      }
      const instancesControl: Array<AbstractControl> = [];
      if (instances) {
        instances.forEach((instance) => {
          instancesControl.push(this.createInstanceFormGroup(instance));
        });
      }
      this.instancesFormGroup.setControl('instances', this.fb.array(instancesControl));
      if (this.disabled) {
        this.instancesFormGroup.disable({emitEvent: false});
      }
      this.valueChange$ = this.instancesFormGroup.valueChanges.subscribe(value => {
        this.updateModel(value.instances);
      });
    }
  }

  private createInstanceFormGroup(instance: Instance): FormGroup {
    return this.fb.group({
      id: [instance.id],
      attributes: [instance.attributes],
      resources: [instance.resources]
    });
  }

  private updateModel(instances: Instance[]) {
    if (instances && this.instancesFormGroup.valid) {
      this.propagateChange(instances);
    } else {
      this.propagateChange(null);
    }
  }

  changeInstanceResourcesCheckBox = (value: boolean, instance: AbstractControl, type: ResourceSettingTelemetry): void => {
    const resources = deepClone(instance.get('resources').value as ResourceLwM2M[]);
    if (value && type === 'observe') {
      resources.forEach(resource => resource[type] = resource.telemetry || resource.attribute);
    } else if (!value && type !== 'observe') {
      resources.forEach(resource => {
        resource[type] = value;
        if (resource.observe && !(resource.telemetry || resource.attribute)) {
          resource.observe = false;
        }
      });
    } else {
      resources.forEach(resource => resource[type] = value);
    }
    instance.get('resources').patchValue(resources);
  }

  private updateValidators(): void {
    this.instancesFormArray.setValidators(this.required ? Validators.required : []);
    this.instancesFormArray.updateValueAndValidity();
  }

  trackByParams = (index: number, instance: Instance): number => {
    return instance.id;
  }

  getIndeterminate = (instance: AbstractControl, type: ResourceSettingTelemetry): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    if (isDefinedAndNotNull(resources)) {
      const checkedResource = resources.filter(resource => resource[type]);
      return checkedResource.length !== 0 && checkedResource.length !== resources.length;
    }
    return false;
  }

  getChecked = (instance: AbstractControl, type: ResourceSettingTelemetry): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    return isDefinedAndNotNull(resources) && resources.every(resource => resource[type]);
  }

  disableObserve(instance: AbstractControl): boolean {
    return this.disabled || !(
      this.getIndeterminate(instance, 'telemetry') ||
      this.getIndeterminate(instance, 'attribute') ||
      this.getChecked(instance, 'telemetry') ||
      this.getChecked(instance, 'attribute')
    );
  }

  get isExpend(): boolean {
    return this.instancesFormArray.length === 1;
  }

  getNameInstance(instance: Instance): string {
    return `${this.translate.instant('device-profile.lwm2m.instance')} #${instance.id}`;
  }

  disableObserveInstance = (instance: AbstractControl): boolean => {
    const checkedAttrTelemetry = this.observeInstance(instance);
    if (checkedAttrTelemetry) {
      instance.get('attributes').patchValue(null, {emitEvent: false});
    }
    return checkedAttrTelemetry;
  }


  observeInstance = (instance: AbstractControl): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    if (isDefinedAndNotNull(resources)) {
      const checkedAttribute = resources.filter(resource => resource.attribute);
      const checkedTelemetry = resources.filter(resource => resource.telemetry);
      return checkedAttribute.length === 0 && checkedTelemetry.length === 0;
    }
    return false;
  }
}

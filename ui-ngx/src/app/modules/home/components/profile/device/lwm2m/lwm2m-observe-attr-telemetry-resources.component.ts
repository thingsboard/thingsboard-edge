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
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { ResourceLwM2M } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { combineLatest, Subject } from 'rxjs';
import { startWith, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resources.component.html',
  styleUrls: ['./lwm2m-observe-attr-telemetry-resources.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourcesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourcesComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryResourcesComponent implements ControlValueAccessor, OnDestroy, Validator {

  resourcesFormGroup: UntypedFormGroup;

  @Input()
  disabled = false;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.resourcesFormGroup = this.fb.group({
      resources: this.fb.array([])
    });

    this.resourcesFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel(this.resourcesFormGroup.getRawValue().resources));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnTouched(fn: any): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  writeValue(value: ResourceLwM2M[]): void {
    this.updatedResources(value);
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.resourcesFormGroup.disable({emitEvent: false});
    } else {
      this.resourcesFormArray.controls.forEach(resource => {
        resource.get('id').enable({emitEvent: false});
        resource.get('name').enable({emitEvent: false});
        resource.get('keyName').enable({emitEvent: false});
        resource.get('attribute').enable({emitEvent: false});
        resource.get('telemetry').enable({onlySelf: true});
        resource.get('attributes').enable({emitEvent: false});
      });
    }
  }

  validate(): ValidationErrors | null {
    return this.resourcesFormGroup.valid ? null : {
      resources: false
    };
  }

  get resourcesFormArray(): UntypedFormArray {
    return this.resourcesFormGroup.get('resources') as UntypedFormArray;
  }

  getNameResourceLwm2m(resourceLwM2M: ResourceLwM2M): string {
    return `#${resourceLwM2M.id} ${resourceLwM2M.name}`;
  }

  private updatedResources(resources: ResourceLwM2M[]): void {
    if (resources.length === this.resourcesFormArray.length) {
      this.resourcesFormArray.patchValue(resources, {onlySelf: true, emitEvent: false});
    } else {
      const resourcesControl: Array<AbstractControl> = [];
      if (resources) {
        resources.forEach((resource) => {
          resourcesControl.push(this.createdResourceFormGroup(resource));
        });
      }
      this.resourcesFormGroup.setControl('resources', this.fb.array(resourcesControl), {emitEvent: false});
      if (this.disabled) {
        this.resourcesFormGroup.disable({emitEvent: false});
      }
    }
  }

  private createdResourceFormGroup(resource: ResourceLwM2M): UntypedFormGroup {
    const form = this.fb.group( {
      id: [resource.id],
      name: [resource.name],
      attribute: [resource.attribute],
      telemetry: [resource.telemetry],
      observe: [resource.observe],
      keyName: [resource.keyName, [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      attributes: [resource.attributes]
    });
    combineLatest([
      form.get('attribute').valueChanges.pipe(startWith(resource.attribute), takeUntil(this.destroy$)),
      form.get('telemetry').valueChanges.pipe(startWith(resource.telemetry), takeUntil(this.destroy$))
    ]).subscribe(([attribute, telemetry]) => {
      if (!this.disabled) {
        if (attribute || telemetry) {
          form.get('observe').enable({emitEvent: false});
        } else {
          form.get('observe').disable({emitEvent: false});
          form.get('observe').patchValue(false, {emitEvent: false});
          form.get('attributes').patchValue({}, {emitEvent: false});
        }
      }
    });
    return form;
  }

  private updateModel(value: ResourceLwM2M[]) {
    if (value && this.resourcesFormGroup.valid) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  trackByParams(index: number, resource: ResourceLwM2M): number {
    return resource.id;
  }

  isDisabledObserve(index: number): boolean{
    return this.resourcesFormArray.at(index).get('observe').disabled;
  }
}

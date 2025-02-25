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

import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { TargetDevice, TargetDeviceType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { IAliasController } from '@core/api/widget-api.models';
import { EntityAliasSelectCallbacks } from '@home/components/alias/entity-alias-select.component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-target-device',
  templateUrl: './target-device.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TargetDeviceComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TargetDeviceComponent),
      multi: true,
    }
  ]
})
export class TargetDeviceComponent implements ControlValueAccessor, OnInit, Validator {

  public get aliasController(): IAliasController {
    return this.widgetConfigComponent.aliasController;
  }

  public get entityAliasSelectCallbacks(): EntityAliasSelectCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  public get targetDeviceOptional(): boolean {
    return this.widgetConfigComponent.modelValue?.typeParameters?.targetDeviceOptional;
  }

  targetDeviceType = TargetDeviceType;

  entityType = EntityType;

  @Input()
  disabled: boolean;

  widgetEditMode = this.utils.widgetEditMode;

  targetDeviceFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (!this.targetDeviceFormGroup.valid) {
      setTimeout(() => {
        this.targetDeviceUpdated(this.targetDeviceFormGroup.value);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.targetDeviceFormGroup.disable({emitEvent: false});
    } else {
      this.targetDeviceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  ngOnInit() {
    this.targetDeviceFormGroup = this.fb.group({
      type: [null, (!this.widgetEditMode && !this.targetDeviceOptional) ? [Validators.required] : []],
      deviceId: [null, (!this.widgetEditMode && !this.targetDeviceOptional) ? [Validators.required] : []],
      entityAliasId: [null, (!this.widgetEditMode && !this.targetDeviceOptional) ? [Validators.required] : []]
    });
    this.targetDeviceFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.targetDeviceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.targetDeviceUpdated(this.targetDeviceFormGroup.value);
      }
    );
  }

  writeValue(targetDevice?: TargetDevice): void {
    this.targetDeviceFormGroup.patchValue({
      type: targetDevice?.type || TargetDeviceType.device,
      deviceId: targetDevice?.deviceId,
      entityAliasId: targetDevice?.entityAliasId
    }, {emitEvent: false});
    this.updateValidators();
    if (targetDevice?.type === TargetDeviceType.entity && targetDevice?.entityAliasId) {
      const entityAliases = this.aliasController.getEntityAliases();
      if (!entityAliases[targetDevice.entityAliasId]) {
        this.targetDeviceFormGroup.get('entityAliasId').patchValue(null, {emitEvent: false});
        setTimeout(() => {
          this.targetDeviceUpdated(this.targetDeviceFormGroup.value);
        }, 0);
      }
    }
  }

  validate(c: UntypedFormControl) {
    return (this.targetDeviceFormGroup.valid) ? null : {
      targetDevice: {
        valid: false,
      },
    };
  }

  private targetDeviceUpdated(targetDevice: TargetDevice) {
    this.propagateChange(targetDevice);
  }

  private updateValidators() {
    const type: TargetDeviceType = this.targetDeviceFormGroup.get('type').value;
    if (!this.widgetEditMode && type) {
      if (type === TargetDeviceType.device) {
        this.targetDeviceFormGroup.get('deviceId').enable({emitEvent: false});
        this.targetDeviceFormGroup.get('entityAliasId').disable({emitEvent: false});
      } else {
        this.targetDeviceFormGroup.get('deviceId').disable({emitEvent: false});
        this.targetDeviceFormGroup.get('entityAliasId').enable({emitEvent: false});
      }
    } else {
      this.targetDeviceFormGroup.get('deviceId').disable({emitEvent: false});
      this.targetDeviceFormGroup.get('entityAliasId').disable({emitEvent: false});
    }
  }

}

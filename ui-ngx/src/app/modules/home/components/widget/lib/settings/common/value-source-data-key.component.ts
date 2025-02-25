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

import { ChangeDetectorRef, Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IAliasController } from '@core/api/widget-api.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKey, Datasource, DatasourceType } from '@app/shared/models/widget.models';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import {
  ValueSourceConfig,
  ValueSourceType,
  ValueSourceTypes,
  ValueSourceTypeTranslation
} from '@shared/models/widget-settings.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-value-source-data-key',
  templateUrl: './value-source-data-key.component.html',
  styleUrls: ['value-source-data-key.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ValueSourceDataKeyComponent),
      multi: true
    }
  ]
})
export class ValueSourceDataKeyComponent extends PageComponent implements OnInit, ControlValueAccessor {

  dataKeyType = DataKeyType;
  datasourceType = DatasourceType;

  valueSourceDataKeyType = ValueSourceType;
  valueSourceDataKeyTypes = ValueSourceTypes;
  valueSourceDataKeyTypeTranslation = ValueSourceTypeTranslation;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  @Input()
  dataKeyCallbacks: DataKeysCallbacks;

  @Input()
  datasource: Datasource;

  valueSourceFormGroup: UntypedFormGroup;

  latestKeyFormControl: UntypedFormControl;
  entityKeyFormControl: UntypedFormControl;

  private modelValue: ValueSourceConfig;

  private propagateChange = (_val: any) => {};

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.valueSourceFormGroup = this.fb.group({
      type: [ValueSourceType.constant, []],
      value: [null, [Validators.required]],
      entityAlias: [null, [Validators.required]]
    });
    this.latestKeyFormControl = this.fb.control(null, [Validators.required]);
    this.entityKeyFormControl = this.fb.control(null, [Validators.required]);
    this.valueSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.latestKeyFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.entityKeyFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.valueSourceFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.valueSourceFormGroup.disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else {
      this.valueSourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ValueSourceConfig): void {
    this.modelValue = value;
    this.valueSourceFormGroup.patchValue(
      {
        type: value.type,
        value: value.value,
        entityAlias: value.entityAlias
      }, {emitEvent: false}
    );
    if (value.type === ValueSourceType.latestKey) {
      this.latestKeyFormControl.patchValue({
        type: value.latestKeyType,
        name: value.latestKey
      }, {emitEvent: false});
    } else if (value.type === ValueSourceType.entity) {
      this.entityKeyFormControl.patchValue({
        type: value.entityKeyType,
        name: value.entityKey
      }, {emitEvent: false});
    }

    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateModel() {
    const value: ValueSourceConfig = this.valueSourceFormGroup.value;
    this.modelValue.type = value.type;
    this.modelValue.value = value.value;
    this.modelValue.entityAlias = value.entityAlias;

    if (value.type === ValueSourceType.latestKey) {
      const latestKey: DataKey = this.latestKeyFormControl.value;
      this.modelValue.latestKey = latestKey?.name;
      this.modelValue.latestKeyType = (latestKey?.type as any);
    } else if (value.type === ValueSourceType.entity) {
      const entityKey: DataKey = this.entityKeyFormControl.value;
      this.modelValue.entityKey = entityKey?.name;
      this.modelValue.entityKeyType = (entityKey?.type as any);
    }
    this.propagateChange(this.modelValue);
  }

  private updateValidators(): void {
    const type: ValueSourceType = this.valueSourceFormGroup.get('type').value;
    if (type === ValueSourceType.constant) {
      this.valueSourceFormGroup.get('value').enable({emitEvent: false});
      this.valueSourceFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceType.latestKey) {
      this.valueSourceFormGroup.get('value').disable({emitEvent: false});
      this.valueSourceFormGroup.get('entityAlias').disable({emitEvent: false});
      this.latestKeyFormControl.enable({emitEvent: false});
      this.entityKeyFormControl.disable({emitEvent: false});
    } else if (type === ValueSourceType.entity) {
      this.valueSourceFormGroup.get('value').disable({emitEvent: false});
      this.valueSourceFormGroup.get('entityAlias').enable({emitEvent: false});
      this.latestKeyFormControl.disable({emitEvent: false});
      this.entityKeyFormControl.enable({emitEvent: false});
    }
  }

}

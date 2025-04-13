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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdditionalMapDataSourceSettings, updateDataKeyToNewDsType } from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { genNextLabelForDataKeys } from '@core/utils';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
  selector: 'tb-additional-map-data-source-row',
  templateUrl: './additional-map-data-source-row.component.html',
  styleUrls: ['./additional-map-data-source-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AdditionalMapDataSourceRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class AdditionalMapDataSourceRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;
  DataKeyType = DataKeyType;

  EntityType = EntityType;

  widgetType = widgetType;

  datasourceTypes: Array<DatasourceType> = [];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  @Output()
  dataSourceRemoved = new EventEmitter();

  dataSourceFormGroup: UntypedFormGroup;

  generateAdditionalDataKey = this.generateDataKey.bind(this);

  modelValue: AdditionalMapDataSourceSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    if (this.context.functionsOnly) {
      this.datasourceTypes = [DatasourceType.function];
    } else {
      this.datasourceTypes = [DatasourceType.function, DatasourceType.device, DatasourceType.entity];
    }
    this.dataSourceFormGroup = this.fb.group({
      dsType: [null, [Validators.required]],
      dsLabel: [null, []],
      dsDeviceId: [null, [Validators.required]],
      dsEntityAliasId: [null, [Validators.required]],
      dataKeys: [null, [Validators.required]]
    });
    this.dataSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.dataSourceFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (newDsType: DatasourceType) => this.onDsTypeChanged(newDsType)
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.dataSourceFormGroup.disable({emitEvent: false});
    } else {
      this.dataSourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: AdditionalMapDataSourceSettings): void {
    this.modelValue = value;
    this.dataSourceFormGroup.patchValue(
      {
        dsType: value?.dsType,
        dsLabel: value?.dsLabel,
        dsDeviceId: value?.dsDeviceId,
        dsEntityAliasId: value?.dsEntityAliasId,
        dataKeys: value?.dataKeys
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  private generateDataKey(key: DataKey): DataKey {
    const dataKey = this.context.callbacks.generateDataKey(key.name, key.type, null, false, null);
    const dataKeys: DataKey[] = this.dataSourceFormGroup.get('dataKeys').value || [];
    dataKey.label = genNextLabelForDataKeys(dataKey.label, dataKeys);
    return dataKey;
  }

  private onDsTypeChanged(newDsType: DatasourceType) {
    let updateModel = false;
    const dataKeys: DataKey[] = this.dataSourceFormGroup.get('dataKeys').value;
    if (dataKeys?.length) {
      for (const key of dataKeys) {
        updateModel = updateDataKeyToNewDsType(key, newDsType) || updateModel;
      }
      if (updateModel) {
        this.dataSourceFormGroup.get('dataKeys').patchValue(dataKeys, {emitEvent: false});
      }
    }
    this.updateValidators();
    if (updateModel) {
      this.updateModel();
    }
  }

  private updateValidators() {
    const dsType: DatasourceType = this.dataSourceFormGroup.get('dsType').value;
    if (dsType === DatasourceType.function) {
      this.dataSourceFormGroup.get('dsLabel').enable({emitEvent: false});
      this.dataSourceFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else if (dsType === DatasourceType.device) {
      this.dataSourceFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataSourceFormGroup.get('dsLabel').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataSourceFormGroup.value};
    this.propagateChange(this.modelValue);
  }
}

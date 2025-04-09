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
import { MapDataSourceSettings } from '@shared/models/widget/maps/map.models';
import { DatasourceType, datasourceTypeTranslationMap, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
  selector: 'tb-map-data-source-row',
  templateUrl: './map-data-source-row.component.html',
  styleUrls: ['./map-data-source-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapDataSourceRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapDataSourceRowComponent implements ControlValueAccessor, OnInit {

  DatasourceType = DatasourceType;

  EntityType = EntityType;

  widgetType = widgetType;

  datasourceTypes: Array<DatasourceType> = [DatasourceType.device, DatasourceType.entity];
  datasourceTypesTranslations = datasourceTypeTranslationMap;

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  @Output()
  dataSourceRemoved = new EventEmitter();

  dataSourceFormGroup: UntypedFormGroup;

  modelValue: MapDataSourceSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.dataSourceFormGroup = this.fb.group({
      dsType: [null, [Validators.required]],
      dsDeviceId: [null, [Validators.required]],
      dsEntityAliasId: [null, [Validators.required]],
      dsFilterId: [null, []]
    });
    this.dataSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.dataSourceFormGroup.get('dsType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateValidators()
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

  writeValue(value: MapDataSourceSettings): void {
    this.modelValue = value;
    this.dataSourceFormGroup.patchValue(
      {
        dsType: value?.dsType,
        dsDeviceId: value?.dsDeviceId,
        dsEntityAliasId: value?.dsEntityAliasId,
        dsFilterId: value?.dsFilterId
      }, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }


  private updateValidators() {
    const dsType: DatasourceType = this.dataSourceFormGroup.get('dsType').value;
    if (dsType === DatasourceType.device) {
      this.dataSourceFormGroup.get('dsDeviceId').enable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').disable({emitEvent: false});
    } else {
      this.dataSourceFormGroup.get('dsDeviceId').disable({emitEvent: false});
      this.dataSourceFormGroup.get('dsEntityAliasId').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = {...this.modelValue, ...this.dataSourceFormGroup.value};
    this.propagateChange(this.modelValue);
  }
}

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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
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
import { mergeDeep } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AdditionalMapDataSourceSettings,
  additionalMapDataSourceValid,
  additionalMapDataSourceValidator,
  defaultAdditionalMapDataSourceSettings
} from '@shared/models/widget/maps/map.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
  selector: 'tb-map-data-sources',
  templateUrl: './map-data-sources.component.html',
  styleUrls: ['./map-data-sources.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapDataSourcesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapDataSourcesComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapDataSourcesComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  dataSourcesFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.dataSourcesFormGroup = this.fb.group({
      dataSources: [this.fb.array([]), []]
    });
    this.dataSourcesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let dataSources: AdditionalMapDataSourceSettings[] = this.dataSourcesFormGroup.get('dataSources').value;
        if (dataSources) {
          dataSources = dataSources.filter(dataSource => additionalMapDataSourceValid(dataSource));
        }
        this.propagateChange(dataSources);
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
      this.dataSourcesFormGroup.disable({emitEvent: false});
    } else {
      this.dataSourcesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AdditionalMapDataSourceSettings[] | undefined): void {
    const dataSources: AdditionalMapDataSourceSettings[] = value || [];
    this.dataSourcesFormGroup.setControl('dataSources', this.prepareDataSourcesFormArray(dataSources), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.dataSourcesFormGroup.valid;
    return valid ? null : {
      dataSources: {
        valid: false,
      },
    };
  }

  dataSourcesFormArray(): UntypedFormArray {
    return this.dataSourcesFormGroup.get('dataSources') as UntypedFormArray;
  }

  trackByDataSource(index: number, dataSourceControl: AbstractControl): any {
    return dataSourceControl;
  }

  removeDataSource(index: number) {
    (this.dataSourcesFormGroup.get('dataSources') as UntypedFormArray).removeAt(index);
  }

  addDataSource() {
    const dataSource = mergeDeep<AdditionalMapDataSourceSettings>({} as AdditionalMapDataSourceSettings,
      defaultAdditionalMapDataSourceSettings(this.context.functionsOnly));
    const dataSourcesArray = this.dataSourcesFormGroup.get('dataSources') as UntypedFormArray;
    const dataSourceControl = this.fb.control(dataSource, [additionalMapDataSourceValidator]);
    dataSourcesArray.push(dataSourceControl);
  }

  private prepareDataSourcesFormArray(dataSources: AdditionalMapDataSourceSettings[]): UntypedFormArray {
    const dataSourcesControls: Array<AbstractControl> = [];
    dataSources.forEach((dataSource) => {
      dataSourcesControls.push(this.fb.control(dataSource, [additionalMapDataSourceValidator]));
    });
    return this.fb.array(dataSourcesControls);
  }
}

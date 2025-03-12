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
  defaultMapDataLayerSettings,
  MapDataLayerSettings,
  MapDataLayerType,
  mapDataLayerValid,
  mapDataLayerValidator,
  MapType
} from '@shared/models/widget/maps/map.models';
import { MapSettingsComponent } from '@home/components/widget/lib/settings/common/map/map-settings.component';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
  selector: 'tb-map-data-layers',
  templateUrl: './map-data-layers.component.html',
  styleUrls: ['./map-data-layers.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapDataLayersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapDataLayersComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapDataLayersComponent implements ControlValueAccessor, OnInit, Validator {

  MapType = MapType;

  @Input()
  disabled: boolean;

  @Input()
  mapType: MapType = MapType.geoMap;

  @Input()
  dataLayerType: MapDataLayerType = 'markers';

  @Input()
  context: MapSettingsContext;

  dataLayersFormGroup: UntypedFormGroup;

  addDataLayerText: string;

  noDataLayersText: string;

  private propagateChange = (_val: any) => {};

  constructor(private mapSettingsComponent: MapSettingsComponent,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    switch (this.dataLayerType) {
      case 'trips':
        this.addDataLayerText = 'widgets.maps.data-layer.trip.add-trip';
        this.noDataLayersText = 'widgets.maps.data-layer.trip.no-trips';
        break;
      case 'markers':
        this.addDataLayerText = 'widgets.maps.data-layer.marker.add-marker';
        this.noDataLayersText = 'widgets.maps.data-layer.marker.no-markers';
        break;
      case 'polygons':
        this.addDataLayerText = 'widgets.maps.data-layer.polygon.add-polygon';
        this.noDataLayersText = 'widgets.maps.data-layer.polygon.no-polygons';
        break;
      case 'circles':
        this.addDataLayerText = 'widgets.maps.data-layer.circle.add-circle';
        this.noDataLayersText = 'widgets.maps.data-layer.circle.no-circles';
        break;
    }
    this.dataLayersFormGroup = this.fb.group({
      dataLayers: [this.fb.array([]), []]
    });
    this.dataLayersFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let layers: MapDataLayerSettings[] = this.dataLayersFormGroup.get('dataLayers').value;
        if (layers) {
          layers = layers.filter(layer => mapDataLayerValid(layer, this.dataLayerType));
        }
        this.propagateChange(layers);
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
      this.dataLayersFormGroup.disable({emitEvent: false});
    } else {
      this.dataLayersFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapDataLayerSettings[] | undefined): void {
    const dataLayers: MapDataLayerSettings[] = value || [];
    this.dataLayersFormGroup.setControl('dataLayers', this.prepareDataLayersFormArray(dataLayers), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.dataLayersFormGroup.valid;
    return valid ? null : {
      dataLayers: {
        valid: false,
      },
    };
  }

  dataLayersFormArray(): UntypedFormArray {
    return this.dataLayersFormGroup.get('dataLayers') as UntypedFormArray;
  }

  trackByDataLayer(index: number, dataLayerControl: AbstractControl): any {
    return dataLayerControl;
  }

  removeDataLayer(index: number) {
    (this.dataLayersFormGroup.get('dataLayers') as UntypedFormArray).removeAt(index);
  }

  addDataLayer() {
    const dataLayer = mergeDeep<MapDataLayerSettings>({} as MapDataLayerSettings,
      defaultMapDataLayerSettings(this.mapType, this.dataLayerType, this.context.functionsOnly));
    const dataLayersArray = this.dataLayersFormGroup.get('dataLayers') as UntypedFormArray;
    const dataLayerControl = this.fb.control(dataLayer, [mapDataLayerValidator(this.dataLayerType)]);
    dataLayersArray.push(dataLayerControl);
  }

  private prepareDataLayersFormArray(dataLayers: MapDataLayerSettings[]): UntypedFormArray {
    const dataLayersControls: Array<AbstractControl> = [];
    dataLayers.forEach((dataLayer) => {
      dataLayersControls.push(this.fb.control(dataLayer, [mapDataLayerValidator(this.dataLayerType)]));
    });
    return this.fb.array(dataLayersControls);
  }
}

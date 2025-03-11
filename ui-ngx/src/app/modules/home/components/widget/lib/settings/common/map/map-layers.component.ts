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
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  defaultMapLayerSettings,
  MapLayerSettings,
  mapLayerValid,
  mapLayerValidator,
  MapProvider
} from '@shared/models/widget/maps/map.models';

@Component({
  selector: 'tb-map-layers',
  templateUrl: './map-layers.component.html',
  styleUrls: ['./map-layers.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapLayersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapLayersComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapLayersComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  layersFormGroup: UntypedFormGroup;

  get dragEnabled(): boolean {
    return this.layersFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.layersFormGroup = this.fb.group({
      layers: [this.fb.array([]), []]
    });
    this.layersFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let layers: MapLayerSettings[] = this.layersFormGroup.get('layers').value;
        if (layers) {
          layers = layers.filter(layer => mapLayerValid(layer));
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
      this.layersFormGroup.disable({emitEvent: false});
    } else {
      this.layersFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapLayerSettings[] | undefined): void {
    const layers: MapLayerSettings[] = value || [];
    this.layersFormGroup.setControl('layers', this.prepareLayersFormArray(layers), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.layersFormGroup.valid;
    return valid ? null : {
      layers: {
        valid: false,
      },
    };
  }

  layerDrop(event: CdkDragDrop<string[]>) {
    const layersArray = this.layersFormGroup.get('layers') as UntypedFormArray;
    const layer = layersArray.at(event.previousIndex);
    layersArray.removeAt(event.previousIndex);
    layersArray.insert(event.currentIndex, layer);
  }

  layersFormArray(): UntypedFormArray {
    return this.layersFormGroup.get('layers') as UntypedFormArray;
  }

  trackByLayer(index: number, layerControl: AbstractControl): any {
    return layerControl;
  }

  removeLayer(index: number) {
    (this.layersFormGroup.get('layers') as UntypedFormArray).removeAt(index);
  }

  addLayer() {
    const layer = mergeDeep<MapLayerSettings>({} as MapLayerSettings,
      defaultMapLayerSettings(MapProvider.openstreet));
    const layersArray = this.layersFormGroup.get('layers') as UntypedFormArray;
    const layerControl = this.fb.control(layer, [mapLayerValidator]);
    layersArray.push(layerControl);
  }

  private prepareLayersFormArray(layers: MapLayerSettings[]): UntypedFormArray {
    const layersControls: Array<AbstractControl> = [];
    layers.forEach((layer) => {
      layersControls.push(this.fb.control(layer, [mapLayerValidator]));
    });
    return this.fb.array(layersControls);
  }
}

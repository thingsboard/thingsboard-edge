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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  defaultLayerTitle,
  defaultMapLayerSettings,
  googleMapLayerTranslationMap,
  googleMapLayerTypes,
  hereLayerTranslationMap,
  hereLayerTypes,
  MapLayerSettings,
  MapProvider,
  mapProviders,
  mapProviderTranslationMap,
  openStreetLayerTypes,
  openStreetMapLayerTranslationMap,
  tencentLayerTranslationMap,
  tencentLayerTypes
} from '@shared/models/widget/maps/map.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-map-layer-settings-panel',
  templateUrl: './map-layer-settings-panel.component.html',
  providers: [],
  styleUrls: ['./map-layer-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapLayerSettingsPanelComponent implements OnInit {

  MapProvider = MapProvider;

  mapProviders = mapProviders;

  mapProviderTranslationMap = mapProviderTranslationMap;

  openStreetLayerTypes = openStreetLayerTypes;

  openStreetMapLayerTranslationMap = openStreetMapLayerTranslationMap;

  googleMapLayerTypes = googleMapLayerTypes;

  googleMapLayerTranslationMap = googleMapLayerTranslationMap;

  hereLayerTypes = hereLayerTypes;

  hereLayerTranslationMap = hereLayerTranslationMap;

  tencentLayerTypes = tencentLayerTypes;

  tencentLayerTranslationMap = tencentLayerTranslationMap;

  @Input()
  mapLayerSettings: MapLayerSettings;

  @Input()
  popover: TbPopoverComponent<MapLayerSettingsPanelComponent>;

  @Output()
  mapLayerSettingsApplied = new EventEmitter<MapLayerSettings>();

  layerFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.layerFormGroup = this.fb.group(
      {
        label: [null, []],
        provider: [null, [Validators.required]],
        layerType: [null, [Validators.required]],
        tileUrl: [null, [Validators.required]],
        apiKey: [null, [Validators.required]]
      }
    );
    this.layerFormGroup.patchValue(
      this.mapLayerSettings, {emitEvent: false}
    );
    this.layerFormGroup.get('provider').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((newProvider: MapProvider) => {
      this.onProviderChanged(newProvider);
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  labelPlaceholder(): string {
    let translationKey = defaultLayerTitle(this.layerFormGroup.value);
    if (!translationKey) {
      translationKey = 'widget-config.set';
    }
    return this.translate.instant(translationKey);
  }

  applyLayerSettings() {
    const layerSettings: MapLayerSettings = this.layerFormGroup.value;
    this.mapLayerSettingsApplied.emit(layerSettings);
  }

  private onProviderChanged(newProvider: MapProvider) {
    let modelValue: MapLayerSettings = this.layerFormGroup.value;
    modelValue = {...defaultMapLayerSettings(newProvider), label: modelValue.label};
    this.layerFormGroup.patchValue(
      modelValue, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const provider: MapProvider = this.layerFormGroup.get('provider').value;
    if (provider === MapProvider.custom) {
      this.layerFormGroup.get('tileUrl').enable({emitEvent: false});
      this.layerFormGroup.get('layerType').disable({emitEvent: false});
    } else {
      this.layerFormGroup.get('tileUrl').disable({emitEvent: false});
      this.layerFormGroup.get('layerType').enable({emitEvent: false});
    }
    if ([MapProvider.google, MapProvider.here].includes(provider)) {
      this.layerFormGroup.get('apiKey').enable({emitEvent: false});
    } else {
      this.layerFormGroup.get('apiKey').disable({emitEvent: false});
    }
  }
}

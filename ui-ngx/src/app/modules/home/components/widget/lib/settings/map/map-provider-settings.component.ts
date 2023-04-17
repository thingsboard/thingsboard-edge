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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup, NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  GoogleMapProviderSettings,
  HereMapProviderSettings,
  ImageMapProviderSettings,
  MapProviders,
  MapProviderSettings,
  mapProviderTranslationMap,
  OpenStreetMapProviderSettings,
  TencentMapProviderSettings
} from '@home/components/widget/lib/maps/map-models';
import { extractType } from '@core/utils';
import { keys } from 'ts-transformer-keys';
import { IAliasController } from '@core/api/widget-api.models';

@Component({
  selector: 'tb-map-provider-settings',
  templateUrl: './map-provider-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapProviderSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapProviderSettingsComponent),
      multi: true
    }
  ]
})
export class MapProviderSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  aliasController: IAliasController;

  @Input()
  disabled: boolean;

  @Input()
  ignoreImageMap = false;

  private modelValue: MapProviderSettings;

  private propagateChange = null;

  public providerSettingsFormGroup: UntypedFormGroup;

  mapProviders = Object.values(MapProviders);

  mapProvider = MapProviders;

  mapProviderTranslations = mapProviderTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    if (this.ignoreImageMap) {
      this.mapProviders = this.mapProviders.filter((provider) => provider !== MapProviders.image);
    }
    this.providerSettingsFormGroup = this.fb.group({
      provider: [null, [Validators.required]],
      googleProviderSettings: [null, []],
      openstreetProviderSettings: [null, []],
      hereProviderSettings: [null, []],
      imageMapProviderSettings: [null, []],
      tencentMapProviderSettings: [null, []]
    });
    this.providerSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.providerSettingsFormGroup.get('provider').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.providerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.providerSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MapProviderSettings): void {
    this.modelValue = value;
    const provider = value?.provider;
    const googleProviderSettings = extractType<GoogleMapProviderSettings>(value, keys<GoogleMapProviderSettings>());
    const openstreetProviderSettings = extractType<OpenStreetMapProviderSettings>(value, keys<OpenStreetMapProviderSettings>());
    const hereProviderSettings = extractType<HereMapProviderSettings>(value, keys<HereMapProviderSettings>());
    const imageMapProviderSettings = extractType<ImageMapProviderSettings>(value, keys<ImageMapProviderSettings>());
    const tencentMapProviderSettings = extractType<TencentMapProviderSettings>(value, keys<TencentMapProviderSettings>());
    this.providerSettingsFormGroup.patchValue(
      {
        provider,
        googleProviderSettings,
        openstreetProviderSettings,
        hereProviderSettings,
        imageMapProviderSettings,
        tencentMapProviderSettings
      }, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.providerSettingsFormGroup.valid ? null : {
      mapProviderSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: {
      provider: MapProviders,
      googleProviderSettings: GoogleMapProviderSettings,
      openstreetProviderSettings: OpenStreetMapProviderSettings,
      hereProviderSettings: HereMapProviderSettings,
      imageMapProviderSettings: ImageMapProviderSettings,
      tencentMapProviderSettings: TencentMapProviderSettings
    } = this.providerSettingsFormGroup.value;
    this.modelValue = {
      provider: value.provider,
      ...value.googleProviderSettings,
      ...value.openstreetProviderSettings,
      ...value.hereProviderSettings,
      ...value.imageMapProviderSettings,
      ...value.tencentMapProviderSettings
    };
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const provider: MapProviders = this.providerSettingsFormGroup.get('provider').value;
    this.providerSettingsFormGroup.disable({emitEvent: false});
    this.providerSettingsFormGroup.get('provider').enable({emitEvent: false});
    switch (provider) {
      case MapProviders.google:
        this.providerSettingsFormGroup.get('googleProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('googleProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.openstreet:
        this.providerSettingsFormGroup.get('openstreetProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('openstreetProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.here:
        this.providerSettingsFormGroup.get('hereProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('hereProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.image:
        this.providerSettingsFormGroup.get('imageMapProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('imageMapProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
      case MapProviders.tencent:
        this.providerSettingsFormGroup.get('tencentMapProviderSettings').enable({emitEvent});
        this.providerSettingsFormGroup.get('tencentMapProviderSettings').updateValueAndValidity({emitEvent: false});
        break;
    }
  }
}

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
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { TranslateService } from '@ngx-translate/core';
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
import { deepClone } from '@core/utils';
import {
  MapLayerSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/map-layer-settings-panel.component';

@Component({
  selector: 'tb-map-layer-row',
  templateUrl: './map-layer-row.component.html',
  styleUrls: ['./map-layer-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapLayerRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MapLayerRowComponent implements ControlValueAccessor, OnInit {

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
  disabled: boolean;

  @Output()
  layerRemoved = new EventEmitter();

  layerFormGroup: UntypedFormGroup;

  modelValue: MapLayerSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.layerFormGroup = this.fb.group({
      label: [null, []],
      provider: [null, [Validators.required]],
      layerType: [null, [Validators.required]],
      tileUrl: [null, [Validators.required]],
      apiKey: [null, [Validators.required]],
      referenceLayer: [null, []]
    });
    this.layerFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.layerFormGroup.get('provider').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((newProvider: MapProvider) => {
      this.onProviderChanged(newProvider);
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.layerFormGroup.disable({emitEvent: false});
    } else {
      this.layerFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: MapLayerSettings): void {
    this.modelValue = value;
    this.layerFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  labelPlaceholder(): string {
    let translationKey = defaultLayerTitle(this.modelValue);
    if (!translationKey) {
      translationKey = 'widget-config.set';
    }
    return this.translate.instant(translationKey);
  }

  editLayer($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        mapLayerSettings: deepClone(this.modelValue)
      };
      const mapLayerSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, MapLayerSettingsPanelComponent, ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], false, null,
        ctx,
        {},
        {}, {}, true);
      mapLayerSettingsPanelPopover.tbComponentRef.instance.popover = mapLayerSettingsPanelPopover;
      mapLayerSettingsPanelPopover.tbComponentRef.instance.mapLayerSettingsApplied.subscribe((layer) => {
        mapLayerSettingsPanelPopover.hide();
        this.layerFormGroup.patchValue(
          layer,
          {emitEvent: false});
        this.updateValidators();
        this.updateModel();
      });
    }
  }

  private onProviderChanged(newProvider: MapProvider) {
    this.modelValue = {...defaultMapLayerSettings(newProvider), label: this.modelValue.label};
    this.layerFormGroup.patchValue(
      this.modelValue, {emitEvent: false}
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

  private updateModel() {
    this.modelValue = this.layerFormGroup.value;
    this.propagateChange(this.modelValue);
  }
}

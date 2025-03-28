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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
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
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarkerClusteringSettings } from '@shared/models/widget/maps/map.models';
import { merge } from 'rxjs';

@Component({
  selector: 'tb-marker-clustering-settings',
  templateUrl: './marker-clustering-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MarkerClusteringSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MarkerClusteringSettingsComponent),
      multi: true
    }
  ]
})
export class MarkerClusteringSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  settingsExpanded = false;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  @Input()
  disabled: boolean;

  private modelValue: MarkerClusteringSettings;

  private propagateChange = null;

  public clusteringSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    this.clusteringSettingsFormGroup = this.fb.group({
      enable: [null, []],
      zoomOnClick: [null, []],
      maxZoom: [null, [Validators.min(0), Validators.max(18)]],
      maxClusterRadius: [null, [Validators.min(0)]],
      zoomAnimation: [null, []],
      showCoverageOnHover: [null, []],
      spiderfyOnMaxZoom: [null, []],
      chunkedLoad: [null, []],
      lazyLoad: [null, []],
      useClusterMarkerColorFunction: [null, []],
      clusterMarkerColorFunction: [null, []]
    });
    this.clusteringSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.clusteringSettingsFormGroup.get('enable').valueChanges,
          this.clusteringSettingsFormGroup.get('useClusterMarkerColorFunction').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
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
      this.clusteringSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.clusteringSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: MarkerClusteringSettings): void {
    this.modelValue = value;
    this.clusteringSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.settingsExpanded = this.clusteringSettingsFormGroup.get('enable').value;
    this.updateValidators();
    this.clusteringSettingsFormGroup.get('enable').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((enable) => {
      this.settingsExpanded = enable;
    });
  }

  public validate(c: UntypedFormControl) {
    const valid = this.clusteringSettingsFormGroup.valid;
    return valid ? null : {
      markerClustering: {
        valid: false,
      },
    };
  }

  private updateValidators() {
    const enable: boolean = this.clusteringSettingsFormGroup.get('enable').value;
    const useClusterMarkerColorFunction: boolean = this.clusteringSettingsFormGroup.get('useClusterMarkerColorFunction').value;
    if (enable) {
      this.clusteringSettingsFormGroup.enable({emitEvent: false});
      if (!useClusterMarkerColorFunction) {
        this.clusteringSettingsFormGroup.get('clusterMarkerColorFunction').disable({emitEvent: false});
      }
    } else {
      this.clusteringSettingsFormGroup.disable({emitEvent: false});
      this.clusteringSettingsFormGroup.get('enable').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.clusteringSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}

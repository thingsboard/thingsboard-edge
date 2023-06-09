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
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MapEditorSettings } from '@home/components/widget/lib/maps/map-models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-map-editor-settings',
  templateUrl: './map-editor-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MapEditorSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MapEditorSettingsComponent),
      multi: true
    }
  ]
})
export class MapEditorSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: MapEditorSettings;

  private propagateChange = null;

  public mapEditorSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.mapEditorSettingsFormGroup = this.fb.group({
      snappable: [null, []],
      initDragMode: [null, []],
      hideAllControlButton: [null, []],
      hideDrawControlButton: [null, []],
      hideEditControlButton: [null, []],
      hideRemoveControlButton: [null, []],
    });
    this.mapEditorSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.mapEditorSettingsFormGroup.get('hideAllControlButton').valueChanges.subscribe(() => {
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
      this.mapEditorSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.mapEditorSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators(false);
    }
  }

  writeValue(value: MapEditorSettings): void {
    this.modelValue = value;
    this.mapEditorSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.mapEditorSettingsFormGroup.valid ? null : {
      mapEditorSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: MapEditorSettings = this.mapEditorSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const hideAllControlButton: boolean = this.mapEditorSettingsFormGroup.get('hideAllControlButton').value;
    if (hideAllControlButton) {
      this.mapEditorSettingsFormGroup.get('hideDrawControlButton').disable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideEditControlButton').disable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideRemoveControlButton').disable({emitEvent});
    } else {
      this.mapEditorSettingsFormGroup.get('hideDrawControlButton').enable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideEditControlButton').enable({emitEvent});
      this.mapEditorSettingsFormGroup.get('hideRemoveControlButton').enable({emitEvent});
    }
    this.mapEditorSettingsFormGroup.get('hideDrawControlButton').updateValueAndValidity({emitEvent: false});
    this.mapEditorSettingsFormGroup.get('hideEditControlButton').updateValueAndValidity({emitEvent: false});
    this.mapEditorSettingsFormGroup.get('hideRemoveControlButton').updateValueAndValidity({emitEvent: false});
  }
}

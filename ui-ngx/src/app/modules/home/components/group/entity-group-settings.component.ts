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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { PageComponent } from '@shared/components/page.component';
import {
  EntityGroupDetailsMode,
  entityGroupDetailsModeTranslationMap,
  EntityGroupSettings,
  groupSettingsDefaults
} from '@shared/models/entity-group.models';

@Component({
  selector: 'tb-entity-group-settings',
  templateUrl: './entity-group-settings.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityGroupSettingsComponent),
    multi: true
  }]
})
export class EntityGroupSettingsComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: EntityGroupSettings | null;

  settingsFormGroup: FormGroup;

  @Input()
  entityType: EntityType;

  @Input()
  disabled: boolean;

  entityGroupDetailsModes = Object.keys(EntityGroupDetailsMode);
  entityGroupDetailsModeTranslations = entityGroupDetailsModeTranslationMap;

  entityTypes = EntityType;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.settingsFormGroup = this.fb.group(groupSettingsDefaults(this.entityType, {} as EntityGroupSettings));
    this.settingsFormGroup.get('defaultPageSize').setValidators([Validators.min(5)]);
    this.settingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.settingsFormGroup.disable({emitEvent: false})
    } else {
      this.settingsFormGroup.enable({emitEvent: false})
    }
  }

  writeValue(value: EntityGroupSettings | null): void {
    this.modelValue = groupSettingsDefaults(this.entityType, value || {} as EntityGroupSettings);
    this.settingsFormGroup.reset(this.modelValue,{emitEvent: false});
  }

  private propagateChange = (v: any) => { };

  private updateModel() {
    if (this.settingsFormGroup.valid) {
      const value = this.settingsFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}

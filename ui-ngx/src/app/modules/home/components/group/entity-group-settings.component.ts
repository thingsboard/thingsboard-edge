///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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

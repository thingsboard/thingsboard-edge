///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { AfterViewInit, Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntityId } from '@shared/models/id/entity-id';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupInfo, entityGroupTypes } from '@shared/models/entity-group.models';

@Component({
  selector: 'tb-entity-group-select',
  templateUrl: './entity-group-select.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityGroupSelectComponent),
    multi: true
  }]
})
export class EntityGroupSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entityGroupSelectFormGroup: FormGroup;

  modelValue: string;

  @Input()
  allowedGroupTypes: Array<EntityType>;

  @Input()
  excludeGroupTypes: Array<EntityType>;

  @Input()
  defaultGroupType: EntityType;

  @Input()
  excludeGroupIds: Array<string>;

  @Input()
  excludeGroupAll: boolean;

  @Input()
  placeholderText: string;

  @Input()
  notFoundText: string;

  @Input()
  requiredText: string;

  @Input()
  ownerId: EntityId;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @Output()
  currentGroupType = new EventEmitter<EntityType>();

  @Output()
  currentGroupInfo = new EventEmitter<EntityGroupInfo>();

  displayGroupTypeSelect: boolean;

  entityGroupTypes: Array<EntityType>;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    this.entityGroupTypes = [...entityGroupTypes];

    if (this.allowedGroupTypes && this.allowedGroupTypes.length) {
      this.entityGroupTypes = [];
      entityGroupTypes.forEach((groupType) => {
        if (this.allowedGroupTypes.indexOf(groupType) !== -1) {
          this.entityGroupTypes.push(groupType);
        }
      });
    }

    if (this.excludeGroupTypes && this.excludeGroupTypes.length) {
      this.excludeGroupTypes.forEach((excludeGroupType) => {
        const index = this.entityGroupTypes.indexOf(excludeGroupType);
        if (index > -1) {
          this.entityGroupTypes.splice(index, 1);
        }
      });
    }

    if (this.entityGroupTypes.length === 1) {
      this.displayGroupTypeSelect = false;
      this.defaultGroupType = this.entityGroupTypes[0];
    } else {
      this.displayGroupTypeSelect = true;
    }

    this.entityGroupSelectFormGroup = this.fb.group({
      groupType: [this.defaultGroupType],
      groupId: [null]
    });

    this.currentGroupType.next(this.defaultGroupType);

    this.entityGroupSelectFormGroup.get('groupType').valueChanges.subscribe(
      (value) => {
        this.currentGroupType.next(value);
      }
    );
    this.entityGroupSelectFormGroup.get('groupId').valueChanges.subscribe(
      (groupId) => {
        this.updateView(groupId);
      }
    );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.entityGroupSelectFormGroup.disable();
    } else {
      this.entityGroupSelectFormGroup.enable();
    }
  }

  writeValue(value: EntityId | null): void {
    if (value !== null) {
      this.modelValue = value.id;
      this.entityGroupSelectFormGroup.get('groupId').patchValue(value.id, {emitEvent: true});
    } else {
      this.entityGroupSelectFormGroup.get('groupId').patchValue(null, {emitEvent: true});
    }
  }

  entityGroupLoaded(entityGroup: EntityGroupInfo) {
    this.currentGroupInfo.next(entityGroup);
    setTimeout(() => {
      this.entityGroupSelectFormGroup.get('groupType').patchValue(entityGroup ? entityGroup.type : null, {emitEvent: true});
    }, 0);
  }

  getCurrentGroupType(): EntityType {
    let groupType = this.entityGroupSelectFormGroup.get('groupType').value;
    if (!groupType) {
      groupType = this.defaultGroupType;
    }
    return groupType;
  }

  updateView(groupId: string | null) {
    if (this.modelValue !== groupId) {
      this.modelValue = groupId;
      if (this.modelValue) {
        this.propagateChange({ entityType: EntityType.ENTITY_GROUP, id: this.modelValue });
      } else {
        this.propagateChange(null);
      }
    }
  }
}

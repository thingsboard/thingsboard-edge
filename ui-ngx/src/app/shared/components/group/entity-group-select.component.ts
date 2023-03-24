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

import { AfterViewInit, Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntityId } from '@shared/models/id/entity-id';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupInfo, entityGroupTypes } from '@shared/models/entity-group.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { of } from 'rxjs';

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

  entityGroupSelectFormGroup: UntypedFormGroup;

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

  @Input()
  originator: string;

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
              private entityGroupService: EntityGroupService,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
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

  entityGroupLoaded(entityGroup: EntityInfoData) {
    (entityGroup ? this.entityGroupService.getEntityGroup(entityGroup.id.id, {ignoreLoading: true}) : of(null)).subscribe(
      (loadedEntityGroup) => {
        this.currentGroupInfo.next(loadedEntityGroup);
        this.entityGroupSelectFormGroup.get('groupType').patchValue(loadedEntityGroup ? loadedEntityGroup.type : null, {emitEvent: true});
      }
    );
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

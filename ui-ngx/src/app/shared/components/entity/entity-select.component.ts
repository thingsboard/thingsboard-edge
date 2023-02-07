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

import { AfterViewInit, Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntityId } from '@shared/models/id/entity-id';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Operation } from '@shared/models/security.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';

@Component({
  selector: 'tb-entity-select',
  templateUrl: './entity-select.component.html',
  styleUrls: ['./entity-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntitySelectComponent),
    multi: true
  }]
})
export class EntitySelectComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  entitySelectFormGroup: UntypedFormGroup;

  modelValue: EntityId = {entityType: null, id: null};

  @Input()
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  useAliasEntityTypes: boolean;

  @Input()
  operation: Operation;

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

  displayEntityTypeSelect: boolean;

  AliasEntityType = AliasEntityType;

  private readonly defaultEntityType: EntityType | AliasEntityType = null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {

    const entityTypes = this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes,
                                                                         this.useAliasEntityTypes,
                                                                         this.operation);
    if (entityTypes.length === 1) {
      this.displayEntityTypeSelect = false;
      this.defaultEntityType = entityTypes[0];
    } else {
      this.displayEntityTypeSelect = true;
    }

    this.entitySelectFormGroup = this.fb.group({
      entityType: [this.defaultEntityType],
      entityId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.entitySelectFormGroup.get('entityType').valueChanges.subscribe(
      (value) => {
        this.updateView(value, this.modelValue.id);
      }
    );
    this.entitySelectFormGroup.get('entityId').valueChanges.subscribe(
      (value) => {
        const id = value ? (typeof value === 'string' ? value : value.id) : null;
        this.updateView(this.modelValue.entityType, id);
      }
    );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.entitySelectFormGroup.disable();
    } else {
      this.entitySelectFormGroup.enable();
    }
  }

  writeValue(value: EntityId | null): void {
    if (value != null) {
      if (value.id === NULL_UUID) {
        value.id = null;
      }
      this.modelValue = value;
      this.entitySelectFormGroup.get('entityType').patchValue(value.entityType, {emitEvent: true});
      this.entitySelectFormGroup.get('entityId').patchValue(value, {emitEvent: true});
    } else {
      this.modelValue = {
        entityType: this.defaultEntityType,
        id: null
      };
      this.entitySelectFormGroup.get('entityType').patchValue(this.defaultEntityType, {emitEvent: true});
      this.entitySelectFormGroup.get('entityId').patchValue(null, {emitEvent: true});
    }
  }

  updateView(entityType: EntityType | AliasEntityType | null, entityId: string | null) {
    if (this.modelValue.entityType !== entityType || this.modelValue.id !== entityId) {
      this.modelValue = {
        entityType,
        id: this.modelValue.entityType !== entityType ? null : entityId
      };

      if (this.modelValue.entityType === AliasEntityType.CURRENT_TENANT
        || this.modelValue.entityType === AliasEntityType.CURRENT_USER
        || this.modelValue.entityType === AliasEntityType.CURRENT_USER_OWNER) {
        this.modelValue.id = NULL_UUID;
      } else if (this.modelValue.entityType === AliasEntityType.CURRENT_CUSTOMER && !this.modelValue.id) {
        this.modelValue.id = NULL_UUID;
      }

      if (this.modelValue.entityType && this.modelValue.id) {
        this.propagateChange(this.modelValue);
      } else {
        this.propagateChange(null);
      }
    }
  }
}

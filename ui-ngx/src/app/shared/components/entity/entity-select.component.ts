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

import { AfterViewInit, Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { EntityId } from '@shared/models/id/entity-id';
import { Operation } from '@shared/models/security.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  additionEntityTypes: {[entityType in string]: string} = {};

  displayEntityTypeSelect: boolean;

  AliasEntityType = AliasEntityType;

  entityTypeNullUUID: Set<AliasEntityType | EntityType | string> = new Set([
    AliasEntityType.CURRENT_TENANT, AliasEntityType.CURRENT_USER, AliasEntityType.CURRENT_USER_OWNER
  ]);

  private readonly defaultEntityType: EntityType | AliasEntityType = null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {

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
    this.entitySelectFormGroup.get('entityType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (value) => {
        this.updateView(value, this.modelValue.id);
      }
    );
    this.entitySelectFormGroup.get('entityId').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (value) => {
        const id = value ? (typeof value === 'string' ? value : value.id) : null;
        this.updateView(this.modelValue.entityType, id);
      }
    );
    const additionNullUIIDEntityTypes = Object.keys(this.additionEntityTypes) as string[];
    if (additionNullUIIDEntityTypes.length > 0) {
      additionNullUIIDEntityTypes.forEach((entityType) => this.entityTypeNullUUID.add(entityType));
    }
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
      this.modelValue = {
        entityType: value.entityType,
        id: value.id !== NULL_UUID ? value.id : null
      };
    } else {
      this.modelValue = {
        entityType: this.defaultEntityType,
        id: null
      };
    }
    this.entitySelectFormGroup.get('entityType').patchValue(this.modelValue.entityType, {emitEvent: false});
    this.entitySelectFormGroup.get('entityId').patchValue(this.modelValue, {emitEvent: false});
  }

  updateView(entityType: EntityType | AliasEntityType | null, entityId: string | null) {
    if (this.modelValue.entityType !== entityType || this.modelValue.id !== entityId) {
      this.modelValue = {
        entityType,
        id: this.modelValue.entityType !== entityType ? null : entityId
      };

      if (this.entityTypeNullUUID.has(this.modelValue.entityType)) {
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

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

import { AfterViewInit, Component, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-originator-select',
  templateUrl: './originator-select.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OriginatorSelectComponent),
    multi: true
  }]
})
export class OriginatorSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  originatorFormGroup: FormGroup;

  modelValue: EntityId | null;

  @Input()
  allowedEntityTypes: Array<EntityType>;

  @Input()
  singleEntityText = 'scheduler.single-entity';

  @Input()
  groupOfEntitiesText = 'scheduler.group-of-entities';

  @Input()
  entitiesGroupOwnerText = 'scheduler.entities-group-owner';

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

  currentUser = getCurrentAuthUser(this.store);

  private loadData = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private fb: FormBuilder) {
    this.originatorFormGroup = this.fb.group({
      originator: ['entity'],
      entityOriginatorId: [null],
      groupOriginatorId: [null],
      groupOwnerId: [null]
    });
    this.originatorFormGroup.get('originator').valueChanges.subscribe(
      (originator: string) => {
        if (originator === 'groupTenant' || originator === 'ownerGroup') {
          const originatorId = {
            entityType: EntityType.ENTITY_GROUP,
            id: null
          };
          this.originatorFormGroup.patchValue({
            groupOriginatorId: originatorId,
            groupOwnerId: null
          });
        } else if (originator === 'entity') {
          this.originatorFormGroup.get('entityOriginatorId').patchValue(null);
        }
      }
    );
    this.originatorFormGroup.valueChanges.subscribe((value) => {
      this.updateView(value);
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.originatorFormGroup.disable({emitEvent: false});
    } else {
      this.originatorFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.modelValue = value;
    let originator = 'entity';
    if (this.modelValue && this.modelValue.entityType === EntityType.ENTITY_GROUP) {
      originator = 'groupTenant';
    }
    this.originatorFormGroup.patchValue({
      entityOriginatorId: originator === 'entity' ? value : null,
      groupOriginatorId: originator !== 'entity' ? value : null
    }, {emitEvent: false});
    this.loadData = true;
  }

  entityGroupLoaded(entityGroup: EntityGroupInfo) {
    if (this.loadData && isDefinedAndNotNull(entityGroup)) {
      this.loadData = false;
      if (this.currentUser.authority === Authority.TENANT_ADMIN && entityGroup?.ownerId?.id !== this.currentUser.tenantId) {
        this.originatorFormGroup.patchValue({
          originator: 'ownerGroup',
          groupOwnerId: entityGroup.ownerId,
          groupOriginatorId: entityGroup.id
        }, {emitEvent: false});
      } else {
        this.originatorFormGroup.patchValue({
          originator: 'groupTenant',
          groupOriginatorId: entityGroup.id
        }, {emitEvent: false});
      }
    }
  }

  updateView(value: {originator: string, entityOriginatorId: EntityId, groupOriginatorId: EntityId} | null) {
    let originatorId = null;
    if (value) {
      originatorId = value.originator !== 'entity' ? value.groupOriginatorId : value.entityOriginatorId;
    }
    if (!isEqual(this.modelValue, originatorId)) {
      this.modelValue = originatorId;
      if (this.modelValue && this.modelValue.id) {
        this.propagateChange(this.modelValue);
      } else {
        this.propagateChange(null);
      }
    }
  }
}

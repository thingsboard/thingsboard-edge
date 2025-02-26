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
  AfterViewInit,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-originator-select',
  templateUrl: './originator-select.component.html',
  styleUrls: ['./originator-select.component.scss'],
  encapsulation: ViewEncapsulation.None,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OriginatorSelectComponent),
    multi: true
  }]
})
export class OriginatorSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  originatorFormGroup: UntypedFormGroup;

  modelValue: EntityId | null;

  @Input()
  allowedEntityTypes: Array<EntityType>;

  @Input()
  singleEntityText = 'scheduler.single-entity';

  @Input()
  groupOfEntitiesText = 'scheduler.group-entities';

  @Input()
  entitiesGroupOwnerText = 'scheduler.entities-group-owner';
  headerOptions = [
    {
      name: this.translate.instant(this.singleEntityText),
      value: 'entity'
    },
    {
      name: this.translate.instant(this.groupOfEntitiesText),
      value: 'groupTenant'
    },
    {
      name: this.translate.instant(this.entitiesGroupOwnerText),
      value: 'ownerGroup'
    }
  ];

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

  private destroy$ = new Subject<void>();

  private loadData = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
    this.originatorFormGroup = this.fb.group({
      originator: ['entity'],
      entityOriginatorId: [null],
      groupOriginatorId: [null],
      groupOwnerId: [null]
    });
    this.originatorFormGroup.get('originator').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      (originator: string) => {
        if (originator === 'groupTenant' || originator === 'ownerGroup') {
          const originatorId = {
            entityType: EntityType.ENTITY_GROUP,
            id: null
          };
          this.originatorFormGroup.patchValue({
            groupOriginatorId: originatorId,
            groupOwnerId: null
          }, {emitEvent: false});
        } else if (originator === 'entity') {
          this.originatorFormGroup.get('entityOriginatorId').patchValue(null, {emitEvent: false});
        }
      }
    );
    this.originatorFormGroup.get('groupOwnerId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.originatorFormGroup.patchValue({
          groupOriginatorId: null
        }, {emitEvent: false});
      }
    );
    this.originatorFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
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
    this.destroy$.next();
    this.destroy$.complete();
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
    if (this.modelValue && this.modelValue.entityType === EntityType.ENTITY_GROUP) {
      this.originatorFormGroup.patchValue({
        originator: 'groupTenant',
        groupOriginatorId: this.modelValue
      }, {emitEvent: false});
      this.loadData = true;
    } else {
      this.originatorFormGroup.patchValue({
        originator: 'entity',
        entityOriginatorId: value,
        groupOriginatorId: null
      }, {emitEvent: false});
    }
  }

  entityGroupLoaded(entityGroup: EntityGroupInfo) {
    if (this.loadData) {
      this.loadData = false;
      if (isDefinedAndNotNull(entityGroup)) {
        if (this.currentUser.authority === Authority.TENANT_ADMIN && entityGroup.ownerId?.id !== this.currentUser.tenantId) {
          this.originatorFormGroup.get('originator').patchValue('ownerGroup', {emitEvent: false});
          this.originatorFormGroup.get('groupOwnerId').patchValue(entityGroup.ownerId, {emitEvent: false});
        } else {
          this.originatorFormGroup.get('originator').patchValue('groupTenant', {emitEvent: false});
          this.originatorFormGroup.get('groupOwnerId').patchValue(null, {emitEvent: false});
        }
      } else {
        this.originatorFormGroup.patchValue({
          originator: 'groupTenant',
          groupOwnerId: null,
          groupOriginatorId: null
        }, {emitEvent: true});
      }
    }
  }


  updateView(value: {originator: string; entityOriginatorId: EntityId; groupOriginatorId: EntityId} | null) {
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

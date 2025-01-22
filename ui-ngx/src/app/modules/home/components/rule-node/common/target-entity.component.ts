///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormGroup, NG_VALUE_ACCESSOR, UntypedFormBuilder } from '@angular/forms';
import { EntityService } from '@app/core/http/entity.service';
import { coerceBoolean } from '@app/shared/decorators/coercion';
import { EntityType } from '@app/shared/models/entity-type.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';

@Component({
  selector: 'tb-target-entity',
  templateUrl: './target-entity.component.html',
  styleUrls: ['./target-entity.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TargetEntityComponent),
      multi: true
    }
  ]
})

export class TargetEntityComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input() allowedGroupTypes: Array<EntityType>;

  @Input()
  @coerceBoolean()
  isTypeSelected = false;

  private propagateChange = null;
  public targetEntityControlGroup: FormGroup;
  private destroy$ = new Subject<void>();

  constructor(private fb: UntypedFormBuilder,
              private entityService: EntityService) {
  }

  ngOnInit(): void {
    this.targetEntityControlGroup = this.fb.group({
      entityGroupId: [null, []],
      groupOwnerId: [null, []]
    });

    this.targetEntityControlGroup.get('entityGroupId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.propagateChange(value);
    })

    this.targetEntityControlGroup.get('groupOwnerId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.targetEntityControlGroup.get('entityGroupId').patchValue(null, {emitEvent: false});
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.targetEntityControlGroup.disable({emitEvent: false});
    } else {
      this.targetEntityControlGroup.enable({emitEvent: false});
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(entityId: EntityId): void {
    if (entityId?.entityType === EntityType.ENTITY_GROUP) {
      this.targetEntityControlGroup.get('entityGroupId').patchValue(entityId, {emitEvent: false});
      if (entityId) {
        this.entityService.getEntity(entityId.entityType as EntityType, entityId.id).pipe(
          takeUntil(this.destroy$)
        ).subscribe((value: BaseData<EntityId>) => {
          this.targetEntityControlGroup.get('groupOwnerId').patchValue(value.ownerId, {emitEvent: false});
        });
      }
    }
  }
}

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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { MessageType } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';
import { deepClone, isDefinedAndNotNull, isEqual, isObject, isString } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { OtaUpdateType } from '@shared/models/ota-package.models';

@Component({
  selector: 'tb-ota-update-event-config',
  templateUrl: './ota-update-config-event.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OtaUpdateEventConfigComponent),
    multi: true
  }]
})
export class OtaUpdateEventConfigComponent implements ControlValueAccessor, OnDestroy, OnInit {

  private destroy$ = new Subject<void>();

  modelValue: SchedulerEventConfiguration | null;
  updatePackageForm: UntypedFormGroup;
  currentGroupType: EntityType;
  packageType = OtaUpdateType.FIRMWARE;
  profileId: string;
  groupId: string;
  groupAll = false;

  @Input()
  schedulerEventType: string;

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.updatePackageForm = this.fb.group({
      originatorId: [null, Validators.required],
      packageId: [{value: null, disabled: true}, Validators.required]
    });

    this.updatePackageForm.get('originatorId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((entityId) => {
      if (isDefinedAndNotNull(entityId)) {
        this.updatePackageForm.get('packageId').enable({emitEvent: false});
      } else {
        this.updatePackageForm.get('packageId').disable({emitEvent: false});
        this.updatePackageForm.get('packageId').patchValue(null, {emitEvent: false});
      }
    });

    this.updatePackageForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    if (isDefinedAndNotNull(this.updatePackageForm) && this.schedulerEventType === 'updateSoftware') {
      this.packageType = OtaUpdateType.SOFTWARE;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.updatePackageForm.disable({emitEvent: false});
    } else if (isDefinedAndNotNull(this.updatePackageForm.get('originatorId').value)){
      this.updatePackageForm.enable({emitEvent: false});
    } else {
      this.updatePackageForm.get('originatorId').enable({emitEvent: false});
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = value;
    let doUpdate = false;
    if (this.modelValue) {
      if (!this.modelValue.msgType) {
        this.modelValue.msgType =
          this.schedulerEventType === 'updateSoftware' ? MessageType.SOFTWARE_UPDATED : MessageType.FIRMWARE_UPDATED;
        doUpdate = true;
      }
      let formValue = deepClone(this.modelValue);
      if (!isEqual(this.modelValue.msgBody, {})) {
        formValue = Object.assign(formValue, {packageId: this.modelValue.msgBody});
        this.updatePackageForm.get('packageId').enable({emitEvent: false});
      } else {
        this.updatePackageForm.get('packageId').disable({emitEvent: false});
      }
      delete formValue.msgBody;
      this.updatePackageForm.patchValue(formValue, {emitEvent: false});
    }
    if (doUpdate) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  private updateModel() {
    if (this.updatePackageForm.valid) {
      const value = this.updatePackageForm.getRawValue();
      const msgValue = {
        originatorId: value.originatorId,
        msgBody: value.packageId !== null ? value.packageId : {}
      };
      this.modelValue = {...this.modelValue, ...msgValue};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  currentEntity(entity: EntityId | EntityGroupInfo | null) {
    if (isDefinedAndNotNull(entity)) {
      if (isString(entity.id) && 'entityType' in entity && entity.entityType === EntityType.DEVICE_PROFILE) {
        this.profileId = entity.id;
        this.groupId = null;
        this.groupAll = false;
      } else if (isObject(entity.id) && entity.id.hasOwnProperty('id')) {
        this.groupId = (entity.id as EntityId).id;
        this.groupAll = (entity as EntityGroupInfo).groupAll;
      }
    }
  }
}

///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
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
  selector: 'tb-update-firmware-event-config',
  templateUrl: './update-firmware.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UpdateFirmwareComponent),
    multi: true
  }]
})
export class UpdateFirmwareComponent implements ControlValueAccessor, OnDestroy, OnInit {

  private destroy$ = new Subject();

  modelValue: SchedulerEventConfiguration | null;
  updateFirmwareForm: FormGroup;
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
              private fb: FormBuilder) {
    this.updateFirmwareForm = this.fb.group({
      originatorId: [null, Validators.required],
      firmwareId: [{value: null, disabled: true}, Validators.required]
    });

    this.updateFirmwareForm.get('originatorId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((entityId) => {
      if (isDefinedAndNotNull(entityId)) {
        this.updateFirmwareForm.get('firmwareId').enable({emitEvent: false});
      } else {
        this.updateFirmwareForm.get('firmwareId').disable({emitEvent: false});
        this.updateFirmwareForm.get('firmwareId').patchValue(null, {emitEvent: false});
      }
    });

    this.updateFirmwareForm.valueChanges.pipe(
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
    if (isDefinedAndNotNull(this.updateFirmwareForm) && this.schedulerEventType === 'updateSoftware') {
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
      this.updateFirmwareForm.disable({emitEvent: false});
    } else if (isDefinedAndNotNull(this.updateFirmwareForm.get('originatorId').value)){
      this.updateFirmwareForm.enable({emitEvent: false});
    } else {
      this.updateFirmwareForm.get('originatorId').enable({emitEvent: false});
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
        formValue = Object.assign(formValue, {firmwareId: this.modelValue.msgBody});
        this.updateFirmwareForm.get('firmwareId').enable({emitEvent: false});
      } else {
        this.updateFirmwareForm.get('firmwareId').disable({emitEvent: false});
      }
      delete formValue.msgBody;
      this.updateFirmwareForm.patchValue(formValue, {emitEvent: false});
    }
    if (doUpdate) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  private updateModel() {
    if (this.updateFirmwareForm.valid) {
      const value = this.updateFirmwareForm.getRawValue();
      const msgValue = {
        originatorId: value.originatorId,
        msgBody: value.firmwareId !== null ? value.firmwareId : {}
      };
      this.modelValue = {...this.modelValue, ...msgValue};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  currentEntity(entity: EntityId | EntityGroupInfo | null) {
    if (entity !== null) {
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

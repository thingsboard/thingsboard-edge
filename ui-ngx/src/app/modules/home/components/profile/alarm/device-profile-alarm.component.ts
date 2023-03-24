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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { DeviceProfileAlarmRule, DeviceProfileAlarm, deviceProfileAlarmValidator } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { EntityId } from '@shared/models/id/entity-id';
import { UtilsService } from '@core/services/utils.service';

@Component({
  selector: 'tb-device-profile-alarm',
  templateUrl: './device-profile-alarm.component.html',
  styleUrls: ['./device-profile-alarm.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceProfileAlarmComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceProfileAlarmComponent),
      multi: true,
    }
  ]
})
export class DeviceProfileAlarmComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Output()
  removeAlarm = new EventEmitter();

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  @Input()
  expanded = false;

  @Input()
  deviceProfileId: EntityId;

  private modelValue: DeviceProfileAlarm;

  alarmFormGroup: UntypedFormGroup;

  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private dialog: MatDialog,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.modelValue);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmFormGroup = this.fb.group({
      id: [null, Validators.required],
      alarmType: [null, [Validators.required, Validators.maxLength(255)]],
      createRules: [null],
      clearRule: [null],
      propagate: [null],
      propagateRelationTypes: [null],
      propagateToOwner: [null],
      propagateToOwnerHierarchy: [null],
      propagateToTenant: [null]
    }, { validators: deviceProfileAlarmValidator });
    this.alarmFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmFormGroup.disable({emitEvent: false});
    } else {
      this.alarmFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DeviceProfileAlarm): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (!this.modelValue.alarmType) {
      this.expanded = true;
    }
    this.alarmFormGroup.reset(this.modelValue || undefined, {emitEvent: false});
    if (!this.disabled && !this.alarmFormGroup.valid) {
      this.updateModel();
    }
  }

  public addClearAlarmRule() {
    const clearAlarmRule: DeviceProfileAlarmRule = {
      condition: {
        condition: []
      }
    };
    this.alarmFormGroup.patchValue({clearRule: clearAlarmRule});
  }

  public removeClearAlarmRule() {
    this.alarmFormGroup.patchValue({clearRule: null});
  }

  public validate(c: UntypedFormControl) {
    if (c.parent) {
      const alarmType = c.value.alarmType;
      const profileAlarmsType = [];
      c.parent.getRawValue().forEach((alarm: DeviceProfileAlarm) => {
          profileAlarmsType.push(alarm.alarmType);
        }
      );
      if (profileAlarmsType.filter(profileAlarmType => profileAlarmType === alarmType).length > 1) {
        this.alarmFormGroup.get('alarmType').setErrors({
          unique: true
        });
      }
    }
    return (this.alarmFormGroup.valid) ? null : {
      alarm: {
        valid: false,
      },
    };
  }

  removeRelationType(key: string): void {
    const keys: string[] = this.alarmFormGroup.get('propagateRelationTypes').value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.alarmFormGroup.get('propagateRelationTypes').setValue(keys, {emitEvent: true});
    }
  }

  addRelationType(event: MatChipInputEvent): void {
    const input = event.chipInput.inputElement;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.alarmFormGroup.get('propagateRelationTypes').value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.alarmFormGroup.get('propagateRelationTypes').setValue(keys, {emitEvent: true});
      }
    }
    if (input) {
      input.value = '';
    }
  }

  get alarmTypeTitle(): string {
    const alarmType = this.alarmFormGroup.get('alarmType').value;
    return this.utils.customTranslation(alarmType, alarmType);
  }

  private updateModel() {
    const value = this.alarmFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}

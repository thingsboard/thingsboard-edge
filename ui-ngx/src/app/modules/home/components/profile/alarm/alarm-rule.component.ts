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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
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
import { AlarmRule } from '@shared/models/device.models';
import { MatDialog } from '@angular/material/dialog';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isDefinedAndNotNull } from '@core/utils';
import {
  EditAlarmDetailsDialogComponent,
  EditAlarmDetailsDialogData
} from '@home/components/profile/alarm/edit-alarm-details-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { UtilsService } from '@core/services/utils.service';

@Component({
  selector: 'tb-alarm-rule',
  templateUrl: './alarm-rule.component.html',
  styleUrls: ['./alarm-rule.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmRuleComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AlarmRuleComponent),
      multi: true,
    }
  ]
})
export class AlarmRuleComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  deviceProfileId: EntityId;

  private modelValue: AlarmRule;

  alarmRuleFormGroup: UntypedFormGroup;

  expandAlarmDetails = false;

  private propagateChange = (v: any) => { };

  constructor(private dialog: MatDialog,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.alarmRuleFormGroup = this.fb.group({
      condition: [null, [Validators.required]],
      schedule: [null],
      alarmDetails: [null],
      dashboardId: [null]
    });
    this.alarmRuleFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleFormGroup.disable({emitEvent: false});
    } else {
      this.alarmRuleFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AlarmRule): void {
    this.modelValue = value;
    const model = this.modelValue ? {
      ...this.modelValue,
      dashboardId: this.modelValue.dashboardId?.id
    } : null;
    this.alarmRuleFormGroup.reset(model || undefined, {emitEvent: false});
  }

  public openEditDetailsDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EditAlarmDetailsDialogComponent, EditAlarmDetailsDialogData,
          string>(EditAlarmDetailsDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            alarmDetails: this.alarmRuleFormGroup.get('alarmDetails').value,
            readonly: this.disabled
          }
        }).afterClosed().subscribe((alarmDetails) => {
          if (isDefinedAndNotNull(alarmDetails)) {
            this.alarmRuleFormGroup.patchValue({alarmDetails});
          }
    });
  }

  public validate(c: UntypedFormControl) {
    return (!this.required && !this.modelValue || this.alarmRuleFormGroup.valid) ? null : {
      alarmRule: {
        valid: false,
      },
    };
  }

  get alarmDetailsText(): string {
    const alarmType = this.alarmRuleFormGroup.get('alarmDetails').value;
    return this.utils.customTranslation(alarmType, alarmType);
  }

  private updateModel() {
    const value = this.alarmRuleFormGroup.value;
    if (this.modelValue) {
      this.modelValue = {...this.modelValue, ...value, dashboardId: value.dashboardId ? new DashboardId(value.dashboardId) : null};
      this.propagateChange(this.modelValue);
    }
  }
}

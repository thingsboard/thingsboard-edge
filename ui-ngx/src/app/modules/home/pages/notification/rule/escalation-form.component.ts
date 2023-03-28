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
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import {
  NonConfirmedNotificationEscalation,
  NotificationTarget,
  NotificationType
} from '@shared/models/notification.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntil } from 'rxjs/operators';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'tb-escalation-form',
  templateUrl: './escalation-form.component.html',
  styleUrls: ['./escalation-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EscalationFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EscalationFormComponent),
      multi: true,
    }
  ]
})
export class EscalationFormComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  @Input()
  disabled: boolean;

  @Input()
  systemEscalation = false;

  escalationFormGroup: FormGroup;

  entityType = EntityType;
  notificationType = NotificationType;

  private modelValue;
  private propagateChange = null;
  private propagateChangePending = false;
  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private dialog: MatDialog) {
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
    this.escalationFormGroup = this.fb.group(
      {
        delayInSec: [0],
        targets: [null, Validators.required],
      });
    this.escalationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.escalationFormGroup.disable({emitEvent: false});
    } else {
      this.escalationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: NonConfirmedNotificationEscalation): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    this.modelValue.delayInSec = +value.delayInSec;
    if (isDefinedAndNotNull(this.modelValue)) {
      this.escalationFormGroup.patchValue(this.modelValue, {emitEvent: false});
    }
    if (!this.disabled && !this.escalationFormGroup.valid) {
      this.updateModel();
    }
  }

  createTarget($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          let formValue: string[] = this.escalationFormGroup.get('targets').value;
          if (!formValue) {
            formValue = [];
          }
          formValue.push(res.id.id);
          this.escalationFormGroup.get('targets').patchValue(formValue);
        }
      });
  }

  public validate(c: FormControl) {
    return (this.escalationFormGroup.valid) ? null : {
      escalation: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.escalationFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}

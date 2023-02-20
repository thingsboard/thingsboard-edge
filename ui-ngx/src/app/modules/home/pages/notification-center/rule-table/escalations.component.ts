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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Subject } from 'rxjs';
import { NonConfirmedNotificationEscalation } from '@shared/models/notification.models';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-escalations-component',
  templateUrl: './escalations.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EscalationsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EscalationsComponent),
      multi: true,
    }
  ]
})
export class EscalationsComponent implements ControlValueAccessor, Validator, OnDestroy {

  escalationsFormGroup: FormGroup;
  newEscalation = false;

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

  private mainEscalaion = {
    delayInSec: 0,
    targets: null
  };

  private destroy$ = new Subject<void>();

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.escalationsFormGroup = this.fb.group({
      escalations: this.fb.array([])
    });

    this.escalationsFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  get escalationsFormArray(): FormArray {
    return this.escalationsFormGroup.get('escalations') as FormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.escalationsFormGroup.disable({emitEvent: false});
    } else {
      this.escalationsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(escalations: {[key: string]: Array<string>} | null): void {
    const escalationParse: Array<NonConfirmedNotificationEscalation> = [];
    // tslint:disable-next-line:forin
    for (const escalation in escalations) {
      escalationParse.push({delayInSec: Number(escalation), targets: escalations[escalation]});
    }
    if (escalationParse.length === 0) {
      this.addEscalation();
    } else if (escalationParse?.length === this.escalationsFormArray.length) {
      this.escalationsFormArray.patchValue(escalationParse, {emitEvent: false});
    } else {
      const escalationsControls: Array<AbstractControl> = [];
      if (escalationParse) {
        escalationParse.forEach(escalation => {
          escalationsControls.push(this.fb.control(escalation, [Validators.required]));
        });
      } else {
        escalationsControls.push(this.fb.control(this.mainEscalaion, [Validators.required]));
      }
      this.escalationsFormGroup.setControl('escalations', this.fb.array(escalationsControls), {emitEvent: false});
      if (this.disabled) {
        this.escalationsFormGroup.disable({emitEvent: false});
      } else {
        this.escalationsFormGroup.enable({emitEvent: false});
      }
    }
  }

  public removeEscalation(index: number) {
    (this.escalationsFormGroup.get('escalations') as FormArray).removeAt(index);
  }

  public addEscalation() {
    const escalation = {
      delayInSec: 0,
      targets: null
    };
    this.newEscalation = true;
    const escalationArray = this.escalationsFormGroup.get('escalations') as FormArray;
    escalationArray.push(this.fb.control(escalation, []));
    this.escalationsFormGroup.updateValueAndValidity();
    if (!this.escalationsFormGroup.valid) {
      this.updateModel();
    }
  }

  public validate(c: AbstractControl): ValidationErrors | null {
    return this.escalationsFormGroup.valid ? null : {
      escalation: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const escalations = {};
    this.escalationsFormGroup.get('escalations').value.forEach(
      escalation => escalations[escalation.delayInSec] = escalation.targets
    );
    this.propagateChange(escalations);
  }
}

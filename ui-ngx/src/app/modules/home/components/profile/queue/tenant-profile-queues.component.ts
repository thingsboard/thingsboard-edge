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
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
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
import { QueueInfo } from '@shared/models/queue.models';
import { UtilsService } from '@core/services/utils.service';
import { guid } from '@core/utils';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-tenant-profile-queues',
  templateUrl: './tenant-profile-queues.component.html',
  styleUrls: ['./tenant-profile-queues.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TenantProfileQueuesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TenantProfileQueuesComponent),
      multi: true,
    }
  ]
})
export class TenantProfileQueuesComponent implements ControlValueAccessor, Validator, OnDestroy, OnInit {

  tenantProfileQueuesFormGroup: UntypedFormGroup;
  newQueue = false;
  idMap = [];

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

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private utils: UtilsService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.tenantProfileQueuesFormGroup = this.fb.group({
      queues: this.fb.array([])
    });

    this.tenantProfileQueuesFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get queuesFormArray(): UntypedFormArray {
    return this.tenantProfileQueuesFormGroup.get('queues') as UntypedFormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.newQueue = false;
    if (this.disabled) {
      this.tenantProfileQueuesFormGroup.disable({emitEvent: false});
    } else {
      this.tenantProfileQueuesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(queues: Array<QueueInfo> | null): void {
    if (queues.length === this.queuesFormArray.length) {
      this.queuesFormArray.patchValue(queues, {emitEvent: false});
    } else {
      const queuesControls: Array<AbstractControl> = [];
      if (queues) {
        queues.forEach((queue, index) => {
          if (!queue.id) {
            if (!this.idMap[index]) {
              this.idMap.push(guid());
            }
            queue.id = this.idMap[index];
          }
          queuesControls.push(this.fb.control(queue, [Validators.required]));
        });
      }
      this.tenantProfileQueuesFormGroup.setControl('queues', this.fb.array(queuesControls), {emitEvent: false});
      if (this.disabled) {
        this.tenantProfileQueuesFormGroup.disable({emitEvent: false});
      } else {
        this.tenantProfileQueuesFormGroup.enable({emitEvent: false});
      }
    }
  }

  public trackByQueue(index: number, queueControl: AbstractControl) {
    if (queueControl) {
      return queueControl.value.id;
    }
    return null;
  }

  public removeQueue(index: number) {
    (this.tenantProfileQueuesFormGroup.get('queues') as UntypedFormArray).removeAt(index);
    this.idMap.splice(index, 1);
  }

  public addQueue() {
    const queue = {
      id: guid(),
      consumerPerPartition: false,
      name: '',
      packProcessingTimeout: 2000,
      partitions: 10,
      pollInterval: 25,
      processingStrategy: {
        failurePercentage: 0,
        maxPauseBetweenRetries: 3,
        pauseBetweenRetries: 3,
        retries: 3,
        type: ''
      },
      submitStrategy: {
        batchSize: 0,
        type: ''
      },
      topic: '',
      additionalInfo: {
        description: ''
      }
    };
    this.idMap.push(queue.id);
    this.newQueue = true;
    const queuesArray = this.tenantProfileQueuesFormGroup.get('queues') as UntypedFormArray;
    queuesArray.push(this.fb.control(queue, []));
    this.tenantProfileQueuesFormGroup.updateValueAndValidity();
    if (!this.tenantProfileQueuesFormGroup.valid) {
      this.updateModel();
    }
  }

  getTitle(value): string {
    return this.utils.customTranslation(value, value);
  }

  public validate(c: AbstractControl): ValidationErrors | null {
    return this.tenantProfileQueuesFormGroup.valid ? null : {
      queues: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const queues: Array<QueueInfo> = this.tenantProfileQueuesFormGroup.get('queues').value;
    this.propagateChange(queues);
  }
}

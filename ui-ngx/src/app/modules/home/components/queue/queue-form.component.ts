///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Input, OnInit, Output, EventEmitter, OnDestroy } from '@angular/core';
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
import { MatDialog } from '@angular/material/dialog';
import { UtilsService } from '@core/services/utils.service';
import { QueueInfo, QueueProcessingStrategyTypes, QueueSubmitStrategyTypes } from '@shared/models/queue.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-queue-form',
  templateUrl: './queue-form.component.html',
  styleUrls: ['./queue-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => QueueFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => QueueFormComponent),
      multi: true,
    }
  ]
})
export class QueueFormComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  @Input()
  disabled: boolean;

  @Input()
  newQueue = false;

  @Input()
  mainQueue = false;

  @Input()
  systemQueue = false;

  @Input()
  expanded = false;

  @Output()
  removeQueue = new EventEmitter();

  queueFormGroup: FormGroup;
  submitStrategies: string[] = [];
  processingStrategies: string[] = [];
  queueTitle = '';
  hideBatchSize = false;

  private modelValue: QueueInfo;
  private propagateChange = null;
  private propagateChangePending = false;
  private valueChange$: Subscription = null;

  constructor(private dialog: MatDialog,
              private utils: UtilsService,
              private fb: FormBuilder) {
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
    this.submitStrategies = Object.values(QueueSubmitStrategyTypes);
    this.processingStrategies = Object.values(QueueProcessingStrategyTypes);
    this.queueFormGroup = this.fb.group(
      {
        name: ['', [Validators.required]],
        pollInterval: [25, [Validators.min(1), Validators.required]],
        partitions: [10, [Validators.min(1), Validators.required]],
        consumerPerPartition: [false, []],
        packProcessingTimeout: [2000, [Validators.min(1), Validators.required]],
        submitStrategy: this.fb.group({
          type: [null, [Validators.required]],
          batchSize: [null],
        }),
        processingStrategy: this.fb.group({
          type: [null, [Validators.required]],
          retries: [3, [Validators.min(0), Validators.required]],
          failurePercentage: [ 0, [Validators.min(0), Validators.required, Validators.max(100)]],
          pauseBetweenRetries: [3, [Validators.min(1), Validators.required]],
          maxPauseBetweenRetries: [3, [Validators.min(1), Validators.required]],
        }),
        topic: [''],
        additionalInfo: this.fb.group({
          description: ['']
        })
      });
    this.valueChange$ = this.queueFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.queueFormGroup.get('name').valueChanges.subscribe((value) => {
      this.queueFormGroup.patchValue({topic: `tb_rule_engine.${value}`});
      this.queueTitle = this.utils.customTranslation(value, value);
    });
    this.queueFormGroup.get('submitStrategy').get('type').valueChanges.subscribe(() => {
      this.submitStrategyTypeChanged();
    });
    if (this.newQueue) {
      this.queueFormGroup.get('name').enable({emitEvent: false});
    } else {
      this.queueFormGroup.get('name').disable({emitEvent: false});
    }
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
      this.valueChange$ = null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.queueFormGroup.disable({emitEvent: false});
    } else {
      this.queueFormGroup.enable({emitEvent: false});
      this.queueFormGroup.get('name').disable({emitEvent: false});
    }
  }

  writeValue(value: QueueInfo): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    if (!this.modelValue.name) {
      this.expanded = true;
    }
    this.queueTitle = this.utils.customTranslation(value.name, value.name);
    if (isDefinedAndNotNull(this.modelValue)) {
      this.queueFormGroup.patchValue(this.modelValue, {emitEvent: false});
    }
    this.submitStrategyTypeChanged();
    if (!this.disabled && !this.queueFormGroup.valid) {
      this.updateModel();
    }
  }

  public validate(c: FormControl) {
    if (c.parent && !this.systemQueue) {
      const queueName = c.value.name;
      const profileQueues = [];
      c.parent.getRawValue().forEach((queue) => {
          profileQueues.push(queue.name);
        }
      );
      if (profileQueues.filter(profileQueue => profileQueue === queueName).length > 1) {
        this.queueFormGroup.get('name').setErrors({
          unique: true
        });
      }
    }
    return (this.queueFormGroup.valid) ? null : {
      queue: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.queueFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }

  submitStrategyTypeChanged() {
    const form = this.queueFormGroup.get('submitStrategy') as FormGroup;
    const type: QueueSubmitStrategyTypes = form.get('type').value;
    const batchSizeField = form.get('batchSize');
    if (type === QueueSubmitStrategyTypes.BATCH) {
      batchSizeField.enable({emitEvent: false});
      batchSizeField.patchValue(1000, {emitEvent: false});
      batchSizeField.setValidators([Validators.min(1), Validators.required]);
      this.hideBatchSize = true;
    } else {
      batchSizeField.patchValue(null, {emitEvent: false});
      batchSizeField.disable({emitEvent: false});
      batchSizeField.clearValidators();
      this.hideBatchSize = false;
    }
  }
}

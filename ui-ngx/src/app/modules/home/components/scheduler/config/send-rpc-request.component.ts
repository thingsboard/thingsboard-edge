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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { EntityType } from '@shared/models/entity-type.models';
import { jsonRequired } from '@shared/components/json-object-edit.component';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { safeMerge, sendRPCRequestDefaults } from '@home/components/scheduler/config/send-rpc-request.models';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-send-rpc-request-event-config',
  templateUrl: './send-rpc-request.component.html',
  styleUrls: ['./send-rpc-request.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SendRpcRequestComponent),
    multi: true
  }]
})
export class SendRpcRequestComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: SchedulerEventConfiguration | null;

  sendRpcRequestFormGroup: UntypedFormGroup;

  entityType = EntityType;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    this.sendRpcRequestFormGroup = this.fb.group({
      originatorId: [null, [Validators.required]],
      msgBody: this.fb.group(
        {
          method: [null, [Validators.required, Validators.pattern(/^\S+$/)]],
          params: [null, [jsonRequired]]
        }
      ),
      metadata: this.fb.group(
        {
          oneway: [null, []],
          timeout: [null, [Validators.min(0)]],
          persistent: [null, []]
        }
      )
    });

    this.sendRpcRequestFormGroup.valueChanges.pipe(
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
      this.sendRpcRequestFormGroup.disable({emitEvent: false});
    } else {
      this.sendRpcRequestFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = safeMerge(sendRPCRequestDefaults, value);
    this.sendRpcRequestFormGroup.reset(this.modelValue, { emitEvent: false });
    setTimeout(() => {
      if (isEqual(this.modelValue, sendRPCRequestDefaults)) {
        this.updateModel();
      }
    }, 0);
  }

  private updateModel() {
    if (this.sendRpcRequestFormGroup.valid) {
      const value = this.sendRpcRequestFormGroup.getRawValue();
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  public getMethodValidationText(): string {
    const methodControl = this.sendRpcRequestFormGroup.get('msgBody.method');
    if (methodControl.hasError('required')) {
      return this.translate.instant('scheduler.rpc-method-required');
    } else if (methodControl.hasError('pattern')) {
      return this.translate.instant('scheduler.rpc-method-white-space');
    }

    return '';
  }
}

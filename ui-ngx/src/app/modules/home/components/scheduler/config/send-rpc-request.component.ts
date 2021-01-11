///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { MessageType } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-send-rpc-request-event-config',
  templateUrl: './send-rpc-request.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SendRpcRequestComponent),
    multi: true
  }]
})
export class SendRpcRequestComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: SchedulerEventConfiguration | null;

  sendRpcRequestFormGroup: FormGroup;

  entityType = EntityType;

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.sendRpcRequestFormGroup = this.fb.group({
      originatorId: [null, [Validators.required]],
      msgBody: this.fb.group(
        {
          method: [null, [Validators.required]],
          params: [null, [Validators.required]]
        }
      )
    });
    this.sendRpcRequestFormGroup.valueChanges.subscribe(() => {
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
    this.modelValue = value;
    this.sendRpcRequestFormGroup.reset(undefined,{emitEvent: false});
    let doUpdate = false;
    if (this.modelValue) {
      if (!this.modelValue.msgType) {
        this.modelValue.msgType = MessageType.RPC_CALL_FROM_SERVER_TO_DEVICE
        doUpdate = true;
      }
      if (!this.modelValue.originatorId) {
        this.modelValue.originatorId = {
          entityType: EntityType.DEVICE,
          id: null
        }
        doUpdate = true;
      }
      if (!this.modelValue.metadata || !this.modelValue.metadata.oneway) {
        const metadata = this.modelValue.metadata || {};
        metadata.oneway = true;
        this.modelValue.metadata = metadata;
        doUpdate = true;
      }
      this.sendRpcRequestFormGroup.reset(this.modelValue,{emitEvent: false});
    }
    if (doUpdate) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
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

}

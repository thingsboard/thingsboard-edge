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
import { AttributeScope, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { attributeKeyValueValidator } from '@home/components/scheduler/config/attribute-key-value-table.component';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-update-attributes-event-config',
  templateUrl: './update-attributes.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => UpdateAttributesComponent),
    multi: true
  }]
})
export class UpdateAttributesComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: SchedulerEventConfiguration | null;

  updateAttributesFormGroup: FormGroup;

  currentGroupType: EntityType;

  attributeScopes: AttributeScope[] = [];

  attributeScope = AttributeScope;

  telemetryTypeTranslationsMap = telemetryTypeTranslations;

  entityType = EntityType;

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.attributeScopes.push(AttributeScope.SERVER_SCOPE);
    this.attributeScopes.push(AttributeScope.SHARED_SCOPE);
    this.updateAttributesFormGroup = this.fb.group({
      originatorId: [null, [Validators.required]],
      serverAttributes: [{}, [attributeKeyValueValidator(false)]],
      sharedAttributes: [{}, [attributeKeyValueValidator(false)]],
      metadata: this.fb.group(
        {
          scope: [null, [Validators.required]]
        }
      )
    });

    this.updateAttributesFormGroup.get('originatorId').valueChanges.subscribe(() => {
      const originatorId: EntityId = this.updateAttributesFormGroup.get('originatorId').value;
      if (!originatorId || originatorId.entityType !== EntityType.DEVICE) {
        const scope: AttributeScope = this.updateAttributesFormGroup.get('metadata').get('scope').value;
        if (scope !== AttributeScope.SERVER_SCOPE) {
          this.updateAttributesFormGroup.get('metadata').get('scope').patchValue(AttributeScope.SERVER_SCOPE, {emitEvent: true});
        }
      }
    });

    this.updateAttributesFormGroup.get('metadata').get('scope').valueChanges.subscribe(() => {
      this.updateAttributesFormGroup.get('serverAttributes').patchValue({}, {emitEvent: false});
      this.updateAttributesFormGroup.get('sharedAttributes').patchValue({}, {emitEvent: false});
      this.updateValidators();
    });

    this.updateAttributesFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  private updateValidators() {
    const scope: AttributeScope = this.updateAttributesFormGroup.get('metadata').get('scope').value;
    this.updateAttributesFormGroup.get('serverAttributes')
      .setValidators(attributeKeyValueValidator(scope === AttributeScope.SERVER_SCOPE));
    this.updateAttributesFormGroup.get('sharedAttributes')
      .setValidators(attributeKeyValueValidator(scope === AttributeScope.SHARED_SCOPE));
    this.updateAttributesFormGroup.get('serverAttributes').updateValueAndValidity({emitEvent: false});
    this.updateAttributesFormGroup.get('sharedAttributes').updateValueAndValidity({emitEvent: false});
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
      this.updateAttributesFormGroup.disable({emitEvent: false});
    } else {
      this.updateAttributesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = value;
    this.updateAttributesFormGroup.reset(undefined,{emitEvent: false});
    let doUpdate = false;
    if (this.modelValue) {
      if (!this.modelValue.msgType) {
        this.modelValue.msgType = MessageType.POST_ATTRIBUTES_REQUEST
        doUpdate = true;
      }
      if (!this.modelValue.metadata || !this.modelValue.metadata.scope) {
        const metadata = this.modelValue.metadata || {};
        metadata.scope = AttributeScope.SERVER_SCOPE;
        this.modelValue.metadata = metadata;
        doUpdate = true;
      }
      const formValue = deepClone(this.modelValue);
      const attributes = formValue.msgBody;
      if (formValue.metadata.scope === AttributeScope.SERVER_SCOPE) {
        (formValue as any).serverAttributes = attributes;
      } else if (formValue.metadata.scope === AttributeScope.SHARED_SCOPE) {
        (formValue as any).sharedAttributes = attributes;
      }
      delete formValue.msgBody;
      this.updateAttributesFormGroup.reset(formValue,{emitEvent: false});
    }
    this.updateValidators();
    if (doUpdate) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  private updateModel() {
    if (this.updateAttributesFormGroup.valid) {
      const value = this.updateAttributesFormGroup.getRawValue();
      const scope: AttributeScope = value.metadata.scope;
      if (scope === AttributeScope.SERVER_SCOPE) {
        value.msgBody = value.serverAttributes;
      } else if (scope === AttributeScope.SHARED_SCOPE) {
        value.msgBody = value.sharedAttributes;
      }
      delete value.serverAttributes;
      delete value.sharedAttributes;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}

///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  forwardRef,
  OnDestroy,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator, Validators,
} from '@angular/forms';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  integerRegex,
  MappingValueType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex,
  OPCTypeValue,
  RPCTemplateConfigOPC
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';

@Component({
  selector: 'tb-opc-rpc-parameters',
  templateUrl: './opc-rpc-parameters.component.html',
  styleUrls: ['./opc-rpc-parameters.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OpcRpcParametersComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OpcRpcParametersComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class OpcRpcParametersComponent implements ControlValueAccessor, Validator, OnDestroy {

  rpcParametersFormGroup: UntypedFormGroup;

  readonly valueTypeKeys: MappingValueType[] = Object.values(MappingValueType);
  readonly MappingValueType = MappingValueType;
  readonly valueTypes = mappingValueTypesMap;

  private onChange: (value: RPCTemplateConfigOPC) => void = (_) => {} ;
  private onTouched: () => void = () => {};

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    this.rpcParametersFormGroup = this.fb.group({
      method: [null, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      arguments: this.fb.array([]),
    });

    this.observeValueChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: RPCTemplateConfigOPC) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.rpcParametersFormGroup.valid ? null : {
      rpcParametersFormGroup: { valid: false }
    };
  }

  writeValue(params: RPCTemplateConfigOPC): void {
    this.clearArguments();
    params.arguments?.map(({type, value}) => ({type, [type]: value }))
      .forEach(argument => this.addArgument(argument as OPCTypeValue));
    this.cdr.markForCheck();
    this.rpcParametersFormGroup.get('method').patchValue(params.method);
  }

  private observeValueChanges(): void {
    this.rpcParametersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(params => {
      const updatedArguments = params.arguments.map(({type, ...config}) => ({type, value: config[type]}));
      this.onChange({method: params.method, arguments: updatedArguments});
      this.onTouched();
    });
  }

  removeArgument(index: number): void {
    (this.rpcParametersFormGroup.get('arguments') as FormArray).removeAt(index);
  }

  addArgument(value: OPCTypeValue = {} as OPCTypeValue): void {
    const fromGroup = this.fb.group({
      type: [value.type ?? MappingValueType.STRING],
      string: [
        value.string ?? { value: '', disabled: !(isEqual(value, {}) || value.string)},
        [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]
      ],
      integer: [
        {value: value.integer ?? 0, disabled: !isDefinedAndNotNull(value.integer)},
        [Validators.required, Validators.pattern(integerRegex)]
      ],
      double: [{value: value.double ?? 0, disabled: !isDefinedAndNotNull(value.double)}, [Validators.required]],
      boolean: [{value: value.boolean ?? false, disabled: !isDefinedAndNotNull(value.boolean)}, [Validators.required]],
    });
    this.observeTypeChange(fromGroup);
    (this.rpcParametersFormGroup.get('arguments') as FormArray).push(fromGroup, {emitEvent: false});
  }

  clearArguments(): void {
    const formArray = this.rpcParametersFormGroup.get('arguments') as FormArray;
    while (formArray.length !== 0) {
      formArray.removeAt(0);
    }
  }

  private observeTypeChange(dataKeyFormGroup: FormGroup): void {
    dataKeyFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => {
        dataKeyFormGroup.disable({emitEvent: false});
        dataKeyFormGroup.get('type').enable({emitEvent: false});
        dataKeyFormGroup.get(type).enable({emitEvent: false});
      });
  }
}

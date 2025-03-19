///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  ValidationErrors,
  FormBuilder,
  FormGroup
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import {
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgumentEventValue,
  CalculatedFieldRollingTelemetryArgumentValue,
  CalculatedFieldSingleArgumentValue,
  CalculatedFieldEventArguments,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import {
  JsonObjectEditDialogComponent,
  JsonObjectEditDialogData
} from '@shared/components/dialog/json-object-edit-dialog.component';
import { filter } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-calculated-field-test-arguments',
  templateUrl: './calculated-field-test-arguments.component.html',
  styleUrls: ['./calculated-field-test-arguments.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldTestArgumentsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldTestArgumentsComponent),
      multi: true,
    }
  ]
})
export class CalculatedFieldTestArgumentsComponent extends PageComponent implements ControlValueAccessor, Validator {

  @Input() argumentsTypeMap: Map<string, ArgumentType>;

  argumentsFormArray = this.fb.array<FormGroup>([]);

  readonly entityTypeTranslations = entityTypeTranslations;
  readonly ArgumentTypeTranslations = ArgumentTypeTranslations;
  readonly ArgumentType = ArgumentType;
  readonly CalculatedFieldType = CalculatedFieldType;

  private propagateChange: (value: CalculatedFieldEventArguments) => void = () => {};

  constructor(private fb: FormBuilder, private dialog: MatDialog) {
    super();
    this.argumentsFormArray.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.propagateChange(this.getValue()));
  }

  registerOnChange(propagateChange: (value: CalculatedFieldEventArguments) => void): void {
    this.propagateChange = propagateChange;
  }

  registerOnTouched(_): void {
  }

  writeValue(argumentsObj: CalculatedFieldEventArguments): void {
    this.argumentsFormArray.clear();
    Object.keys(argumentsObj).forEach(key => {
      const value = { ...argumentsObj[key], argumentName: key } as CalculatedFieldArgumentEventValue;
      this.argumentsFormArray.push(this.argumentsTypeMap.get(key) === ArgumentType.Rolling
        ? this.getRollingArgumentFormGroup(value as CalculatedFieldRollingTelemetryArgumentValue)
        : this.getSimpleArgumentFormGroup(value as CalculatedFieldSingleArgumentValue)
      );
    });
  }

  validate(): ValidationErrors | null {
    return this.argumentsFormArray.valid ? null : { arguments: { valid: false } };
  }

  openEditJSONDialog(group: FormGroup): void {
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, CalculatedFieldArgumentEventValue>(JsonObjectEditDialogComponent, {
      disableClose: true,
      height: '760px',
      maxHeight: '70vh',
      minWidth: 'min(700px, 100%)',
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: this.argumentsTypeMap.get(group.get('argumentName').value) === ArgumentType.Rolling ? group.value.rollingJson : group.value,
        required: true,
        fillHeight: true
      }
    }).afterClosed()
      .pipe(filter(Boolean))
      .subscribe(result => this.argumentsTypeMap.get(group.get('argumentName').value) === ArgumentType.Rolling
        ? group.get('rollingJson').patchValue({ values: (result as CalculatedFieldRollingTelemetryArgumentValue).values, timeWindow: (result as CalculatedFieldRollingTelemetryArgumentValue).timeWindow })
        : group.patchValue({ ts: (result as CalculatedFieldSingleArgumentValue).ts, value: (result as CalculatedFieldSingleArgumentValue).value }) );
  }

  private getSimpleArgumentFormGroup({ argumentName, ts, value }: CalculatedFieldSingleArgumentValue): FormGroup {
    return this.fb.group({
      argumentName: [{ value: argumentName, disabled: true}],
      ts: [ts],
      value: [value]
    }) as FormGroup;
  }

  private getRollingArgumentFormGroup({ argumentName, timeWindow, values }: CalculatedFieldRollingTelemetryArgumentValue): FormGroup {
    return this.fb.group({
      argumentName: [{ value: argumentName, disabled: true }],
      rollingJson: [{ values: values ?? [], timeWindow: timeWindow ?? {} }]
    }) as FormGroup;
  }

  private getValue(): CalculatedFieldEventArguments {
    return this.argumentsFormArray.getRawValue().reduce((acc, rowItem) => {
      const { argumentName, rollingJson = {}, ...value } = rowItem;
      acc[argumentName] = { ...rollingJson, ...value };
      return acc;
    }, {});
  }
}

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

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormGroup,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  ModbusDataType,
  ModbusEditableDataTypes,
  ModbusFunctionCodeTranslationsMap,
  ModbusObjectCountByDataType,
  ModbusValue,
  ModbusValueKey,
  noLeadTrailSpacesRegex,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { GatewayHelpLinkPipe } from '@home/components/widget/lib/gateway/pipes/gateway-help-link.pipe';
import { generateSecret } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { TruncateWithTooltipDirective } from '@shared/directives/truncate-with-tooltip.directive';

@Component({
  selector: 'tb-modbus-data-keys-panel',
  templateUrl: './modbus-data-keys-panel.component.html',
  styleUrls: ['./modbus-data-keys-panel.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    GatewayHelpLinkPipe,
    TruncateWithTooltipDirective,
  ]
})
export class ModbusDataKeysPanelComponent implements OnInit, OnDestroy {

  @coerceBoolean()
  @Input() isMaster = false;
  @Input() panelTitle: string;
  @Input() addKeyTitle: string;
  @Input() deleteKeyTitle: string;
  @Input() noKeysText: string;
  @Input() keysType: ModbusValueKey;
  @Input() values: ModbusValue[];
  @Input() popover: TbPopoverComponent<ModbusDataKeysPanelComponent>;

  @Output() keysDataApplied = new EventEmitter<Array<ModbusValue>>();

  keysListFormArray: FormArray<UntypedFormGroup>;
  modbusDataTypes = Object.values(ModbusDataType);
  withFunctionCode = true;
  functionCodesMap = new Map();
  defaultFunctionCodes = [];

  readonly ModbusEditableDataTypes = ModbusEditableDataTypes;
  readonly ModbusFunctionCodeTranslationsMap = ModbusFunctionCodeTranslationsMap;

  private destroy$ = new Subject<void>();

  private readonly defaultReadFunctionCodes = [3, 4];
  private readonly bitsReadFunctionCodes = [1, 2];
  private readonly defaultWriteFunctionCodes = [6, 16];
  private readonly bitsWriteFunctionCodes = [5, 15];

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.withFunctionCode = !this.isMaster || (this.keysType !== ModbusValueKey.ATTRIBUTES && this.keysType !== ModbusValueKey.TIMESERIES);
    this.keysListFormArray = this.prepareKeysFormArray(this.values);
    this.defaultFunctionCodes = this.getDefaultFunctionCodes();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByControlId(_: number, keyControl: AbstractControl): string {
    return keyControl.value.id;
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      tag: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: [{value: '', disabled: !this.isMaster}, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      type: [ModbusDataType.BYTES, [Validators.required]],
      address: [null, [Validators.required]],
      objectsCount: [1, [Validators.required]],
      functionCode: [{ value: this.getDefaultFunctionCodes()[0], disabled: !this.withFunctionCode }, [Validators.required]],
      id: [{value: generateSecret(5), disabled: true}],
    });
    this.observeKeyDataType(dataKeyFormGroup);

    this.keysListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.keysListFormArray.removeAt(index);
    this.keysListFormArray.markAsDirty();
  }

  cancel(): void {
    this.popover.hide();
  }

  applyKeysData(): void {
    this.keysDataApplied.emit(this.keysListFormArray.value);
  }

  private prepareKeysFormArray(values: ModbusValue[]): UntypedFormArray {
    const keysControlGroups: Array<AbstractControl> = [];

    if (values) {
      values.forEach(value => {
        const dataKeyFormGroup = this.createDataKeyFormGroup(value);
        this.observeKeyDataType(dataKeyFormGroup);
        this.functionCodesMap.set(dataKeyFormGroup.get('id').value, this.getFunctionCodes(value.type));

        keysControlGroups.push(dataKeyFormGroup);
      });
    }

    return this.fb.array(keysControlGroups);
  }

  private createDataKeyFormGroup(modbusValue: ModbusValue): FormGroup {
    const { tag, value, type, address, objectsCount, functionCode } = modbusValue;

    return this.fb.group({
      tag: [tag, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: [{ value, disabled: !this.isMaster }, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      type: [type, [Validators.required]],
      address: [address, [Validators.required]],
      objectsCount: [objectsCount, [Validators.required]],
      functionCode: [{ value: functionCode, disabled: !this.withFunctionCode }, [Validators.required]],
      id: [{ value: generateSecret(5), disabled: true }],
    });
  }

  private observeKeyDataType(keyFormGroup: FormGroup): void {
    keyFormGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(dataType => {
      if (!this.ModbusEditableDataTypes.includes(dataType)) {
        keyFormGroup.get('objectsCount').patchValue(ModbusObjectCountByDataType[dataType], {emitEvent: false});
      }
      this.updateFunctionCodes(keyFormGroup, dataType);
    });
  }

  private updateFunctionCodes(keyFormGroup: FormGroup, dataType: ModbusDataType): void {
    const functionCodes = this.getFunctionCodes(dataType);
    this.functionCodesMap.set(keyFormGroup.get('id').value, functionCodes);
    if (!functionCodes.includes(keyFormGroup.get('functionCode').value)) {
      keyFormGroup.get('functionCode').patchValue(functionCodes[0], {emitEvent: false});
    }
  }

  private getFunctionCodes(dataType: ModbusDataType): number[] {
    const writeFunctionCodes = [
      ...(dataType === ModbusDataType.BITS ? this.bitsWriteFunctionCodes : []), ...this.defaultWriteFunctionCodes
    ];

    if (this.keysType === ModbusValueKey.ATTRIBUTES_UPDATES) {
      return writeFunctionCodes.sort((a, b) => a - b);
    }

    const functionCodes = [...this.defaultReadFunctionCodes];
    if (dataType === ModbusDataType.BITS) {
      functionCodes.push(...this.bitsReadFunctionCodes);
    }
    if (this.keysType === ModbusValueKey.RPC_REQUESTS) {
      functionCodes.push(...writeFunctionCodes);
    }

    return functionCodes.sort((a, b) => a - b);
  }

  private getDefaultFunctionCodes(): number[] {
    if (this.keysType === ModbusValueKey.ATTRIBUTES_UPDATES) {
      return this.defaultWriteFunctionCodes;
    }
    if (this.keysType === ModbusValueKey.RPC_REQUESTS) {
      return [...this.defaultReadFunctionCodes, ...this.defaultWriteFunctionCodes];
    }
    return this.defaultReadFunctionCodes;
  }
}

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
  FormControl,
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
  ModifierType,
  ModifierTypesMap,
  noLeadTrailSpacesRegex,
  nonZeroFloat,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { GatewayHelpLinkPipe } from '@home/components/widget/lib/gateway/pipes/gateway-help-link.pipe';
import { generateSecret } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-modbus-data-keys-panel',
  templateUrl: './modbus-data-keys-panel.component.html',
  styleUrls: ['./modbus-data-keys-panel.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    GatewayHelpLinkPipe,
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
  modifierTypes: ModifierType[] = Object.values(ModifierType);
  withFunctionCode = true;

  enableModifiersControlMap = new Map<string, FormControl<boolean>>();
  showModifiersMap = new Map<string, boolean>();
  functionCodesMap = new Map();
  defaultFunctionCodes = [];

  readonly ModbusEditableDataTypes = ModbusEditableDataTypes;
  readonly ModbusFunctionCodeTranslationsMap = ModbusFunctionCodeTranslationsMap;
  readonly ModifierTypesMap = ModifierTypesMap;

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
    const id = generateSecret(5);
    const dataKeyFormGroup = this.fb.group({
      tag: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: [{value: '', disabled: !this.isMaster}, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      type: [ModbusDataType.BYTES, [Validators.required]],
      address: [null, [Validators.required]],
      objectsCount: [1, [Validators.required]],
      functionCode: [{ value: this.getDefaultFunctionCodes()[0], disabled: !this.withFunctionCode }, [Validators.required]],
      modifierType: [{ value: ModifierType.MULTIPLIER, disabled: true }],
      modifierValue: [{ value: 1, disabled: true }, [Validators.pattern(nonZeroFloat)]],
      id: [{value: id, disabled: true}],
    });
    this.showModifiersMap.set(id, false);
    this.enableModifiersControlMap.set(id, this.fb.control(false));
    this.observeKeyDataType(dataKeyFormGroup);
    this.observeEnableModifier(dataKeyFormGroup);

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
    this.keysDataApplied.emit(this.mapKeysWithModifier());
  }

  private mapKeysWithModifier(): Array<ModbusValue> {
    return this.keysListFormArray.value.map((keyData, i) => {
      if (this.showModifiersMap.get(this.keysListFormArray.controls[i].get('id').value)) {
        const { modifierType, modifierValue, ...value } = keyData;
        return modifierType ? { ...value, [modifierType]: modifierValue } : value;
      }
      return keyData;
    });
  }

  private prepareKeysFormArray(values: ModbusValue[]): UntypedFormArray {
    const keysControlGroups: Array<AbstractControl> = [];

    if (values) {
      values.forEach(value => {
        const dataKeyFormGroup = this.createDataKeyFormGroup(value);
        this.observeKeyDataType(dataKeyFormGroup);
        this.observeEnableModifier(dataKeyFormGroup);
        this.functionCodesMap.set(dataKeyFormGroup.get('id').value, this.getFunctionCodes(value.type));

        keysControlGroups.push(dataKeyFormGroup);
      });
    }

    return this.fb.array(keysControlGroups);
  }

  private createDataKeyFormGroup(modbusValue: ModbusValue): FormGroup {
    const { tag, value, type, address, objectsCount, functionCode, multiplier, divider } = modbusValue;
    const id = generateSecret(5);

    const showModifier = this.shouldShowModifier(type);
    this.showModifiersMap.set(id, showModifier);
    this.enableModifiersControlMap.set(id, this.fb.control((multiplier || divider) && showModifier));

    return this.fb.group({
      tag: [tag, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: [{ value, disabled: !this.isMaster }, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      type: [type, [Validators.required]],
      address: [address, [Validators.required]],
      objectsCount: [objectsCount, [Validators.required]],
      functionCode: [{ value: functionCode, disabled: !this.withFunctionCode }, [Validators.required]],
      modifierType: [{
        value: divider ? ModifierType.DIVIDER : ModifierType.MULTIPLIER,
        disabled: !this.enableModifiersControlMap.get(id).value
      }],
      modifierValue: [
        { value: multiplier ?? divider ?? 1, disabled: !this.enableModifiersControlMap.get(id).value },
        [Validators.pattern(nonZeroFloat)]
      ],
      id: [{ value: id, disabled: true }],
    });
  }

  private shouldShowModifier(type: ModbusDataType): boolean {
    return !this.isMaster
      && (this.keysType === ModbusValueKey.ATTRIBUTES || this.keysType === ModbusValueKey.TIMESERIES)
      && (!this.ModbusEditableDataTypes.includes(type));
  }

  private observeKeyDataType(keyFormGroup: FormGroup): void {
    keyFormGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(dataType => {
      if (!this.ModbusEditableDataTypes.includes(dataType)) {
        keyFormGroup.get('objectsCount').patchValue(ModbusObjectCountByDataType[dataType], {emitEvent: false});
      }
      const withModifier = this.shouldShowModifier(dataType);
      this.showModifiersMap.set(keyFormGroup.get('id').value, withModifier);
      this.updateFunctionCodes(keyFormGroup, dataType);
    });
  }

  private observeEnableModifier(keyFormGroup: FormGroup): void {
    this.enableModifiersControlMap.get(keyFormGroup.get('id').value).valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(showModifier => this.toggleModifierControls(keyFormGroup, showModifier));
  }

  private toggleModifierControls(keyFormGroup: FormGroup, enable: boolean): void {
    const modifierTypeControl = keyFormGroup.get('modifierType');
    const modifierValueControl = keyFormGroup.get('modifierValue');

    if (enable) {
      modifierTypeControl.enable();
      modifierValueControl.enable();
    } else {
      modifierTypeControl.disable();
      modifierValueControl.disable();
    }
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

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
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ModbusKeysAddKeyTranslationsMap,
  ModbusKeysDeleteKeyTranslationsMap,
  ModbusKeysNoKeysTextTranslationsMap,
  ModbusKeysPanelTitleTranslationsMap,
  ModbusRegisterTranslationsMap,
  ModbusRegisterType,
  ModbusRegisterValues,
  ModbusValue,
  ModbusValueKey,
  ModbusValues,
  ModbusValuesState,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { EllipsisChipListDirective } from '@shared/directives/ellipsis-chip-list.directive';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ModbusDataKeysPanelComponent } from '../modbus-data-keys-panel/modbus-data-keys-panel.component';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-modbus-values',
  templateUrl: './modbus-values.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusValuesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusValuesComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    EllipsisChipListDirective,
  ],
  styleUrls: ['./modbus-values.component.scss']
})

export class ModbusValuesComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @coerceBoolean()
  @Input() singleMode = false;

  disabled = false;
  modbusRegisterTypes: ModbusRegisterType[] = Object.values(ModbusRegisterType);
  modbusValueKeys = Object.values(ModbusValueKey);
  ModbusValuesTranslationsMap = ModbusRegisterTranslationsMap;
  ModbusValueKey = ModbusValueKey;
  valuesFormGroup: FormGroup;

  private onChange: (value: ModbusValuesState) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.initializeValuesFormGroup();
    this.observeValuesChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: ModbusValuesState) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(values: ModbusValuesState): void {
    if (this.singleMode) {
      this.valuesFormGroup.setValue(this.getSingleRegisterState(values as ModbusValues), { emitEvent: false });
    } else {
      const { holding_registers, coils_initializer, input_registers, discrete_inputs } = values as ModbusRegisterValues;
      this.valuesFormGroup.setValue({
        holding_registers: this.getSingleRegisterState(holding_registers),
        coils_initializer: this.getSingleRegisterState(coils_initializer),
        input_registers: this.getSingleRegisterState(input_registers),
        discrete_inputs: this.getSingleRegisterState(discrete_inputs),
      }, { emitEvent: false });
    }
    this.cdr.markForCheck();
  }

  validate(): ValidationErrors | null {
    return this.valuesFormGroup.valid ? null : {
      valuesFormGroup: {valid: false}
    };
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cdr.markForCheck();
  }

  getValueGroup(valueKey: ModbusValueKey, register?: ModbusRegisterType): FormGroup {
    return register
      ? this.valuesFormGroup.get(register).get(valueKey) as FormGroup
      : this.valuesFormGroup.get(valueKey) as FormGroup;
  }

  manageKeys($event: Event, matButton: MatButton, keysType: ModbusValueKey, register?: ModbusRegisterType): void {
    $event.stopPropagation();
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
      return;
    }

    const keysControl = this.getValueGroup(keysType, register);
    const ctx = {
      values: keysControl.value,
      isMaster: !this.singleMode,
      keysType,
      panelTitle: ModbusKeysPanelTitleTranslationsMap.get(keysType),
      addKeyTitle: ModbusKeysAddKeyTranslationsMap.get(keysType),
      deleteKeyTitle: ModbusKeysDeleteKeyTranslationsMap.get(keysType),
      noKeysText: ModbusKeysNoKeysTextTranslationsMap.get(keysType)
    };
    const dataKeysPanelPopover = this.popoverService.displayPopover(
      trigger,
      this.renderer,
      this.viewContainerRef,
      ModbusDataKeysPanelComponent,
      'leftBottom',
      false,
      null,
      ctx,
      {},
      {},
      {},
      true
    );
    dataKeysPanelPopover.tbComponentRef.instance.popover = dataKeysPanelPopover;
    dataKeysPanelPopover.tbComponentRef.instance.keysDataApplied.pipe(takeUntil(this.destroy$)).subscribe((keysData: ModbusValue[]) => {
      dataKeysPanelPopover.hide();
      keysControl.patchValue(keysData);
      keysControl.markAsDirty();
      this.cdr.markForCheck();
    });
  }

  private initializeValuesFormGroup(): void {
    const getValuesFormGroup = () => this.fb.group(this.modbusValueKeys.reduce((acc, key) => {
      acc[key] = this.fb.control([[], []]);
      return acc;
    }, {}));

    if (this.singleMode) {
      this.valuesFormGroup = getValuesFormGroup();
    } else {
      this.valuesFormGroup = this.fb.group(
        this.modbusRegisterTypes.reduce((registersAcc, register) => {
          registersAcc[register] = getValuesFormGroup();
          return registersAcc;
        }, {})
      );
    }
  }


  private observeValuesChanges(): void {
    this.valuesFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(value);
        this.onTouched();
      });
  }

  private getSingleRegisterState(values: ModbusValues): ModbusValues {
    return {
      attributes: values?.attributes ?? [],
      timeseries: values?.timeseries ?? [],
      attributeUpdates: values?.attributeUpdates ?? [],
      rpc: values?.rpc ?? [],
    };
  }
}

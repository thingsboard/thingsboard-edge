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
  OnChanges,
  OnDestroy,
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
  ModbusValueKey,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { EllipsisChipListDirective } from '@shared/directives/public-api';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ModbusDataKeysPanelComponent } from '../modbus-data-keys-panel/modbus-data-keys-panel.component';

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
  styles: [`
    :host {
      .mat-mdc-tab-body-wrapper {
        min-height: 320px;
      }
    }
  `]
})

export class ModbusValuesComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @Input() singleMode = false;

  modbusRegisterTypes: ModbusRegisterType[] = Object.values(ModbusRegisterType);
  modbusValueKeys = Object.values(ModbusValueKey);
  ModbusValuesTranslationsMap = ModbusRegisterTranslationsMap;
  ModbusValueKey = ModbusValueKey;
  valuesFormGroup: FormGroup;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cdr: ChangeDetectorRef,
  ) {
    this.valuesFormGroup = this.fb.group(this.modbusRegisterTypes.reduce((registersAcc, register) => {
      return {
        ...registersAcc,
        [register]: this.fb.group(this.modbusValueKeys.reduce((acc, key) => ({...acc, [key]: [[], []]}), {})),
      };
    }, {}));

    this.observeValuesChanges();
  }

  ngOnChanges(): void {
    if (this.singleMode) {
      this.valuesFormGroup = this.fb.group(this.modbusValueKeys.reduce((acc, key) => ({...acc, [key]: [[], []]}), {}));
      this.observeValuesChanges();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(values: ModbusRegisterValues): void {
    this.valuesFormGroup.patchValue(values, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.valuesFormGroup.valid ? null : {
      valuesFormGroup: {valid: false}
    };
  }

  getValueGroup(valueKey: ModbusValueKey, register?: ModbusRegisterType) {
    return register ? this.valuesFormGroup.get(register).get(valueKey).value : this.valuesFormGroup.get(valueKey).value;
  }

  manageKeys($event: Event, matButton: MatButton, keysType: ModbusValueKey, register?: ModbusRegisterType): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const group = this.valuesFormGroup;

      const keysControl = register ? group.get(register).get(keysType) : group.get(keysType);
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
      dataKeysPanelPopover.tbComponentRef.instance.keysDataApplied.pipe(takeUntil(this.destroy$)).subscribe((keysData) => {
        dataKeysPanelPopover.hide();
        keysControl.patchValue(keysData);
        keysControl.markAsDirty();
        this.cdr.markForCheck();
      });
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
}

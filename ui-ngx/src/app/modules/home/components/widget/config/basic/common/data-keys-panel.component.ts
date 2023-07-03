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

import {
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKey, DatasourceType, JsonSettingsSchema, widgetType } from '@shared/models/widget.models';
import { dataKeyRowValidator } from '@home/components/widget/config/basic/common/data-key-row.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { alarmFields } from '@shared/models/alarm.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-data-keys-panel',
  templateUrl: './data-keys-panel.component.html',
  styleUrls: ['./data-keys-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataKeysPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DataKeysPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class DataKeysPanelComponent implements ControlValueAccessor, OnInit, OnChanges, Validator {

  @Input()
  disabled: boolean;

  @Input()
  panelTitle: string;

  @Input()
  addKeyTitle: string;

  @Input()
  keySettingsTitle: string;

  @Input()
  removeKeyTitle: string;

  @Input()
  noKeysText: string;

  @Input()
  requiredKeysText: string;

  @Input()
  datasourceType: DatasourceType;

  @Input()
  entityAliasId: string;

  @Input()
  deviceId: string;

  @Input()
  @coerceBoolean()
  hideDataKeyColor = false;

  @Input()
  @coerceBoolean()
  hideSourceSelection = false;

  dataKeyType: DataKeyType;
  alarmKeys: Array<DataKey>;
  functionTypeKeys: Array<DataKey>;

  keysListFormGroup: UntypedFormGroup;

  errorText = '';

  get widgetType(): widgetType {
    return this.widgetConfigComponent.widgetType;
  }

  get callbacks(): DataKeysCallbacks {
    return this.widgetConfigComponent.widgetConfigCallbacks;
  }

  get hasAdditionalLatestDataKeys(): boolean {
    return !this.hideSourceSelection && this.widgetConfigComponent.widgetType === widgetType.timeseries &&
      this.widgetConfigComponent.modelValue?.typeParameters?.hasAdditionalLatestDataKeys;
  }

  get datakeySettingsSchema(): JsonSettingsSchema {
    return this.widgetConfigComponent.modelValue?.dataKeySettingsSchema;
  }

  get dragEnabled(): boolean {
    return this.keysFormArray().controls.length > 1;
  }

  get noKeys(): boolean {
    let keys: DataKey[] = this.keysListFormGroup.get('keys').value;
    if (this.hasAdditionalLatestDataKeys) {
      keys = keys.filter(k => !(k as any).latest);
    }
    return keys.length === 0;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private utils: UtilsService,
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  ngOnInit() {
    this.keysListFormGroup = this.fb.group({
      keys: [this.fb.array([]), []]
    });
    this.keysListFormGroup.valueChanges.subscribe(
      (val) => this.propagateChange(this.keysListFormGroup.get('keys').value)
    );
    this.alarmKeys = [];
    for (const name of Object.keys(alarmFields)) {
      this.alarmKeys.push({
        name,
        type: DataKeyType.alarm
      });
    }
    this.functionTypeKeys = [];
    for (const type of this.utils.getPredefinedFunctionsList()) {
      this.functionTypeKeys.push({
        name: type,
        type: DataKeyType.function
      });
    }
    this.updateParams();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['datasourceType'].includes(propName)) {
            this.updateParams();
        }
      }
    }
  }

  private updateParams() {
    if (this.datasourceType === DatasourceType.function) {
      this.dataKeyType = DataKeyType.function;
    } else {
      if (this.widgetType !== widgetType.latest && this.widgetType !== widgetType.alarm) {
        this.dataKeyType = DataKeyType.timeseries;
      } else {
        this.dataKeyType = null;
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.keysListFormGroup.disable({emitEvent: false});
    } else {
      this.keysListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKey[] | undefined): void {
    this.keysListFormGroup.setControl('keys', this.prepareKeysFormArray(value), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    this.errorText = '';
    let valid = this.keysListFormGroup.valid;
    if (this.noKeys && this.requiredKeysText) {
      valid = false;
      this.errorText = this.requiredKeysText;
    }
    return valid ? null : {
      dataKeyRows: {
        valid: false,
      },
    };
  }

  keyDrop(event: CdkDragDrop<string[]>) {
    const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
    const key = keysArray.at(event.previousIndex);
    keysArray.removeAt(event.previousIndex);
    keysArray.insert(event.currentIndex, key);
  }

  keysFormArray(): UntypedFormArray {
    return this.keysListFormGroup.get('keys') as UntypedFormArray;
  }

  trackByKey(index: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  removeKey(index: number) {
    (this.keysListFormGroup.get('keys') as UntypedFormArray).removeAt(index);
  }

  addKey() {
    const dataKey = this.callbacks.generateDataKey('', null, this.datakeySettingsSchema);
    dataKey.label = '';
    dataKey.decimals = 0;
    if (this.hasAdditionalLatestDataKeys) {
      (dataKey as any).latest = false;
    }
    const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
    const keyControl = this.fb.control(dataKey, [dataKeyRowValidator]);
    keysArray.push(keyControl);
  }

  private prepareKeysFormArray(keys: DataKey[] | undefined): UntypedFormArray {
    const keysControls: Array<AbstractControl> = [];
    if (keys) {
      keys.forEach((key) => {
        keysControls.push(this.fb.control(key, [dataKeyRowValidator]));
      });
    }
    return this.fb.array(keysControls);
  }

}

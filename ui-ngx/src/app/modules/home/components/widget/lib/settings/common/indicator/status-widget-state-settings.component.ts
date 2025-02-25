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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { merge } from 'rxjs';
import {
  StatusWidgetLayout,
  StatusWidgetStateSettings
} from '@home/components/widget/lib/indicator/status-widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-status-widget-state-settings',
  templateUrl: './status-widget-state-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StatusWidgetStateSettingsComponent),
      multi: true
    }
  ]
})
export class StatusWidgetStateSettingsComponent implements OnInit, OnChanges, ControlValueAccessor {

  StatusWidgetLayout = StatusWidgetLayout;

  @Input()
  disabled: boolean;

  @Input()
  layout: StatusWidgetLayout;

  private modelValue: StatusWidgetStateSettings;

  private propagateChange = null;

  public stateSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.stateSettingsFormGroup = this.fb.group({
      showLabel: [null, []],
      label: [null, []],
      labelFont: [null, []],
      showStatus: [null, []],
      status: [null, []],
      statusFont: [null, []],
      icon: [null, []],
      iconSize: [null, []],
      iconSizeUnit: [null, []],
      primaryColor: [null, []],
      secondaryColor: [null, []],
      background: [null, []],
      primaryColorDisabled: [null, []],
      secondaryColorDisabled: [null, []],
      backgroundDisabled: [null, []]
    });
    this.stateSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.stateSettingsFormGroup.get('showLabel').valueChanges,
      this.stateSettingsFormGroup.get('showStatus').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['layout'].includes(propName)) {
          this.updateValidators();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stateSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.stateSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: StatusWidgetStateSettings): void {
    this.modelValue = value;
    this.stateSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    if (this.layout === StatusWidgetLayout.icon) {
      this.stateSettingsFormGroup.get('showLabel').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('label').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('showStatus').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('status').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('statusFont').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColor').disable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColorDisabled').disable({emitEvent: false});
    } else {
      this.stateSettingsFormGroup.get('showLabel').enable({emitEvent: false});
      this.stateSettingsFormGroup.get('showStatus').enable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColor').enable({emitEvent: false});
      this.stateSettingsFormGroup.get('secondaryColorDisabled').enable({emitEvent: false});
      const showLabel: boolean = this.stateSettingsFormGroup.get('showLabel').value;
      const showStatus: boolean = this.stateSettingsFormGroup.get('showStatus').value;
      if (showLabel) {
        this.stateSettingsFormGroup.get('label').enable({emitEvent: false});
        this.stateSettingsFormGroup.get('labelFont').enable({emitEvent: false});
      } else {
        this.stateSettingsFormGroup.get('label').disable({emitEvent: false});
        this.stateSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      }
      if (showStatus) {
        this.stateSettingsFormGroup.get('status').enable({emitEvent: false});
        this.stateSettingsFormGroup.get('statusFont').enable({emitEvent: false});
      } else {
        this.stateSettingsFormGroup.get('status').disable({emitEvent: false});
        this.stateSettingsFormGroup.get('statusFont').disable({emitEvent: false});
      }
    }
  }

  private updateModel() {
    this.modelValue = this.stateSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}

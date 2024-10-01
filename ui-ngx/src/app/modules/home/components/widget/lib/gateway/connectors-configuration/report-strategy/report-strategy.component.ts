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
  Component,
  forwardRef,
  Input,
  OnDestroy,
} from '@angular/core';
import { Subject } from 'rxjs';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {
  ReportStrategyConfig,
  ReportStrategyDefaultValue,
  ReportStrategyType,
  ReportStrategyTypeTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { filter, takeUntil } from 'rxjs/operators';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import {
  ModbusSecurityConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/modbus/modbus-security-config/modbus-security-config.component';
import { coerceBoolean, coerceNumber } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-report-strategy',
  templateUrl: './report-strategy.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReportStrategyComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ReportStrategyComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusSecurityConfigComponent,
  ]
})
export class ReportStrategyComponent implements ControlValueAccessor, OnDestroy {

  @coerceBoolean()
  @Input() isExpansionMode = false;

  @coerceNumber()
  @Input() defaultValue = ReportStrategyDefaultValue.Key;

  reportStrategyFormGroup: UntypedFormGroup;
  showStrategyControl: FormControl<boolean>;

  readonly reportStrategyTypes = Object.values(ReportStrategyType);
  readonly ReportTypeTranslateMap = ReportStrategyTypeTranslationsMap;
  readonly ReportStrategyType = ReportStrategyType;

  private onChange: (value: ReportStrategyConfig) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.showStrategyControl = this.fb.control(false);

    this.reportStrategyFormGroup = this.fb.group({
      type: [{ value: ReportStrategyType.OnReportPeriod, disabled: true }, []],
      reportPeriod: [{ value: this.defaultValue, disabled: true }, [Validators.required]],
    });

    this.observeStrategyFormChange();
    this.observeStrategyToggle();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(reportStrategyConfig: ReportStrategyConfig): void {
    if (this.isExpansionMode) {
      this.showStrategyControl.setValue(!!reportStrategyConfig, {emitEvent: false});
    }
    if (reportStrategyConfig) {
      this.reportStrategyFormGroup.enable({emitEvent: false});
    }
    const { type = ReportStrategyType.OnReportPeriod, reportPeriod = this.defaultValue } = reportStrategyConfig ?? {};
    this.reportStrategyFormGroup.setValue({ type, reportPeriod }, {emitEvent: false});
    this.onTypeChange(type);
  }

  validate(): ValidationErrors | null {
    return this.reportStrategyFormGroup.valid || this.reportStrategyFormGroup.disabled ? null : {
      reportStrategyForm: { valid: false }
    };
  }

  registerOnChange(fn: (value: ReportStrategyConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  private observeStrategyFormChange(): void {
    this.reportStrategyFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.onChange(value);
      this.onTouched();
    });

    this.reportStrategyFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => this.onTypeChange(type));
  }

  private observeStrategyToggle(): void {
    this.showStrategyControl.valueChanges
      .pipe(takeUntil(this.destroy$), filter(() => this.isExpansionMode))
      .subscribe(enable => {
        if (enable) {
          this.reportStrategyFormGroup.enable({emitEvent: false});
          this.reportStrategyFormGroup.get('reportPeriod').addValidators(Validators.required);
          this.onChange(this.reportStrategyFormGroup.value);
        } else {
          this.reportStrategyFormGroup.disable({emitEvent: false});
          this.reportStrategyFormGroup.get('reportPeriod').removeValidators(Validators.required);
          this.onChange(null);
        }
        this.reportStrategyFormGroup.updateValueAndValidity({emitEvent: false});
      });
  }

  private onTypeChange(type: ReportStrategyType): void {
    const reportPeriodControl = this.reportStrategyFormGroup.get('reportPeriod');

    if (type === ReportStrategyType.OnChange) {
      reportPeriodControl.disable({emitEvent: false});
    } else if (!this.isExpansionMode || this.showStrategyControl.value) {
      reportPeriodControl.enable({emitEvent: false});
    }
  }
}

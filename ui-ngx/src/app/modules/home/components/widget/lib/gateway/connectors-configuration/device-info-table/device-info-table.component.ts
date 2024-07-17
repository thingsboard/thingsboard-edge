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
  OnInit,
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  DeviceInfoType,
  noLeadTrailSpacesRegex,
  OPCUaSourceTypes,
  SourceTypes,
  SourceTypeTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-device-info-table',
  templateUrl: './device-info-table.component.html',
  styleUrls: ['./device-info-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceInfoTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeviceInfoTableComponent),
      multi: true
    }
  ]
})
export class DeviceInfoTableComponent extends PageComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  SourceTypeTranslationsMap = SourceTypeTranslationsMap;

  DeviceInfoType = DeviceInfoType;

  @coerceBoolean()
  @Input()
  useSource = true;

  @coerceBoolean()
  @Input()
  required = false;

  @Input()
  sourceTypes: Array<SourceTypes | OPCUaSourceTypes> = Object.values(SourceTypes);

  deviceInfoTypeValue: any;

  get deviceInfoType(): any {
    return this.deviceInfoTypeValue;
  }

  @Input()
  set deviceInfoType(value: any) {
    if (this.deviceInfoTypeValue !== value) {
      this.deviceInfoTypeValue = value;
    }
  }

  mappingFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public dialog: MatDialog,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.mappingFormGroup = this.fb.group({
      deviceNameExpression: ['', this.required ?
        [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)] : [Validators.pattern(noLeadTrailSpacesRegex)]]
    });

    if (this.useSource) {
      this.mappingFormGroup.addControl('deviceNameExpressionSource',
        this.fb.control(this.sourceTypes[0], []));
    }

    if (this.deviceInfoType === DeviceInfoType.FULL) {
      if (this.useSource) {
        this.mappingFormGroup.addControl('deviceProfileExpressionSource',
          this.fb.control(this.sourceTypes[0], []));
      }
      this.mappingFormGroup.addControl('deviceProfileExpression',
        this.fb.control('', this.required ?
          [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)] : [Validators.pattern(noLeadTrailSpacesRegex)]));
    }

    this.mappingFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  writeValue(deviceInfo: any) {
    this.mappingFormGroup.patchValue(deviceInfo, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.mappingFormGroup.valid ? null : {
      mappingForm: { valid: false }
    };
  }

  updateView(value: any) {
    this.propagateChange(value);
  }
}

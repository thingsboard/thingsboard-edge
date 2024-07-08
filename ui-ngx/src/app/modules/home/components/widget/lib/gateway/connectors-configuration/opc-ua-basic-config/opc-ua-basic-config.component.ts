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

import { ChangeDetectionStrategy, Component, forwardRef, Input, OnDestroy, TemplateRef } from '@angular/core';
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
  ConnectorBaseConfig,
  ConnectorType,
  MappingType,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import {
  BrokerConfigControlComponent,
  MappingTableComponent,
  SecurityConfigComponent,
  ServerConfigComponent,
  WorkersConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/public-api';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-opc-ua-basic-config',
  templateUrl: './opc-ua-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OpcUaBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OpcUaBasicConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
    WorkersConfigControlComponent,
    BrokerConfigControlComponent,
    MappingTableComponent,
    ServerConfigComponent,
  ],
  styleUrls: ['./opc-ua-basic-config.component.scss']
})

export class OpcUaBasicConfigComponent implements ControlValueAccessor, Validator, OnDestroy {
  @Input() generalTabContent: TemplateRef<any>;

  mappingTypes = MappingType;
  basicFormGroup: FormGroup;

  onChange!: (value: string) => void;
  onTouched!: () => void;

  protected readonly connectorType = ConnectorType;
  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.basicFormGroup = this.fb.group({
      mapping: [],
      server: [],
    });

    this.basicFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => {
        this.onChange(value);
        this.onTouched();
      });
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

  writeValue(basicConfig: ConnectorBaseConfig): void {
    const editedBase = {
      server: basicConfig.server || {},
      mapping: basicConfig.mapping || [],
    };

    this.basicFormGroup.setValue(editedBase, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.basicFormGroup.valid ? null : {
      basicFormGroup: {valid: false}
    };
  }
}

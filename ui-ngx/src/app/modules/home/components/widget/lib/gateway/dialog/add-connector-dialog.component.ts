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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, UntypedFormControl, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { BaseData, HasId } from '@shared/models/base-data';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  AddConnectorConfigData,
  ConnectorType,
  CreatedConnectorConfigData,
  GatewayConnectorDefaultTypesTranslatesMap,
  GatewayLogLevel,
  getDefaultConfig,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';
import { takeUntil, tap } from "rxjs/operators";

@Component({
  selector: 'tb-add-connector-dialog',
  templateUrl: './add-connector-dialog.component.html',
  styleUrls: ['./add-connector-dialog.component.scss'],
  providers: [],
})
export class AddConnectorDialogComponent extends DialogComponent<AddConnectorDialogComponent, BaseData<HasId>> implements OnInit, OnDestroy {

  connectorForm: UntypedFormGroup;

  connectorType = ConnectorType;

  gatewayConnectorDefaultTypesTranslatesMap = GatewayConnectorDefaultTypesTranslatesMap;
  gatewayLogLevel = Object.values(GatewayLogLevel);

  submitted = false;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddConnectorConfigData,
              public dialogRef: MatDialogRef<AddConnectorDialogComponent, CreatedConnectorConfigData>,
              private fb: FormBuilder,
              private resourcesService: ResourcesService) {
    super(store, router, dialogRef);
    this.connectorForm = this.fb.group({
      type: [ConnectorType.MQTT, []],
      name: ['', [Validators.required, this.uniqNameRequired(), Validators.pattern(noLeadTrailSpacesRegex)]],
      logLevel: [GatewayLogLevel.INFO, []],
      useDefaults: [true, []],
      sendDataOnlyOnChange: [false, []],
      class: ['', []],
      key: ['auto', []],
    });
  }

  ngOnInit(): void {
    this.observeTypeChange();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  helpLinkId(): string {
    return 'https://thingsboard.io/docs/iot-gateway/configuration/';
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    const value = this.connectorForm.getRawValue();
    if (value.useDefaults) {
      getDefaultConfig(this.resourcesService, value.type).subscribe((defaultConfig) => {
        value.configurationJson = defaultConfig;
        if (this.connectorForm.valid) {
          this.dialogRef.close(value);
        }
      });
    } else if (this.connectorForm.valid) {
      this.dialogRef.close(value);
    }
  }

  private uniqNameRequired(): ValidatorFn {
    return (c: UntypedFormControl) => {
      const newName = c.value.trim().toLowerCase();
      const found = this.data.dataSourceData.find((connectorAttr) => {
        const connectorData = connectorAttr.value;
        return connectorData.name.toLowerCase() === newName;
      });
      if (found) {
        if (c.hasError('required')) {
          return c.getError('required');
        }
        return {
          duplicateName: {
            valid: false
          }
        };
      }
      return null;
    };
  }

  private observeTypeChange(): void {
    this.connectorForm.get('type').valueChanges.pipe(
      tap((type: ConnectorType) => {
        const useDefaultControl = this.connectorForm.get('useDefaults');
        if (type === ConnectorType.GRPC || type === ConnectorType.CUSTOM) {
          useDefaultControl.setValue(false);
        } else if (!useDefaultControl.value) {
          useDefaultControl.setValue(true);
        }
      }),
      takeUntil(this.destroy$),
    ).subscribe()
  }
}

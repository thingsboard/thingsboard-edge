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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge } from 'rxjs';
import {
  RpcDataToStateType,
  RpcInitialStateAction,
  rpcInitialStateActions,
  RpcInitialStateSettings,
  rpcInitialStateTranslations
} from '@shared/models/rpc-widget-settings.models';
import { ValueType } from '@shared/models/constants';
import { TargetDevice } from '@shared/models/widget.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslationsShort } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-rpc-initial-state-settings-panel',
  templateUrl: './rpc-initial-state-settings-panel.component.html',
  providers: [],
  styleUrls: ['./rpc-state-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RpcInitialStateSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  initialState: RpcInitialStateSettings<any>;

  @Input()
  stateValueType: ValueType;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  popover: TbPopoverComponent<RpcInitialStateSettingsPanelComponent>;

  @Output()
  initialStateSettingsApplied = new EventEmitter<RpcInitialStateSettings<any>>();

  rpcInitialStateAction = RpcInitialStateAction;

  rpcInitialStateActions = rpcInitialStateActions;

  rpcInitialStateTranslationsMap = rpcInitialStateTranslations;

  telemetryTypeTranslationsMap = telemetryTypeTranslationsShort;

  attributeScopes = Object.keys(AttributeScope) as AttributeScope[];

  dataKeyType = DataKeyType;

  dataToStateType = RpcDataToStateType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  valueType = ValueType;

  initialStateSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.initialStateSettingsFormGroup = this.fb.group(
      {
        action: [this.initialState?.action, []],
        defaultValue: [this.initialState?.defaultValue, [Validators.required]],
        executeRpc: this.fb.group({
          method: [this.initialState?.executeRpc?.method, [Validators.required]],
          requestTimeout: [this.initialState?.executeRpc?.requestTimeout, [Validators.required, Validators.min(5000)]],
          requestPersistent: [this.initialState?.executeRpc?.requestPersistent, []],
          persistentPollingInterval: [this.initialState?.executeRpc?.persistentPollingInterval, [Validators.required, Validators.min(1000)]]
        }),
        getAttribute: this.fb.group({
          scope: [this.initialState?.getAttribute?.scope, []],
          key: [this.initialState?.getAttribute?.key, [Validators.required]],
        }),
        getTimeSeries: this.fb.group({
          key: [this.initialState?.getTimeSeries?.key, [Validators.required]],
        }),
        dataToState: this.fb.group({
          type: [this.initialState?.dataToState?.type, [Validators.required]],
          dataToStateFunction: [this.initialState?.dataToState?.dataToStateFunction, [Validators.required]],
        }),
      }
    );
    if (this.stateValueType === ValueType.BOOLEAN) {
      (this.initialStateSettingsFormGroup.get('dataToState') as UntypedFormGroup).addControl(
        'compareToValue', this.fb.control(this.initialState?.dataToState?.compareToValue, [Validators.required])
      );
    }

    merge(this.initialStateSettingsFormGroup.get('action').valueChanges,
          this.initialStateSettingsFormGroup.get('dataToState').get('type').valueChanges,
          this.initialStateSettingsFormGroup.get('executeRpc').get('requestPersistent').valueChanges).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyInitialStateSettings() {
    const initialStateSettings: RpcInitialStateSettings<any> = this.initialStateSettingsFormGroup.getRawValue();
    this.initialStateSettingsApplied.emit(initialStateSettings);
  }

  private updateValidators() {
    const action: RpcInitialStateAction = this.initialStateSettingsFormGroup.get('action').value;
    const dataToStateType: RpcDataToStateType = this.initialStateSettingsFormGroup.get('dataToState').get('type').value;

    this.initialStateSettingsFormGroup.get('defaultValue').disable({emitEvent: false});
    this.initialStateSettingsFormGroup.get('executeRpc').disable({emitEvent: false});
    this.initialStateSettingsFormGroup.get('getAttribute').disable({emitEvent: false});
    this.initialStateSettingsFormGroup.get('getTimeSeries').disable({emitEvent: false});
    switch (action) {
      case RpcInitialStateAction.DO_NOTHING:
        this.initialStateSettingsFormGroup.get('defaultValue').enable({emitEvent: false});
        break;
      case RpcInitialStateAction.EXECUTE_RPC:
        this.initialStateSettingsFormGroup.get('executeRpc').enable({emitEvent: false});
        const requestPersistent: boolean = this.initialStateSettingsFormGroup.get('executeRpc').get('requestPersistent').value;
        if (requestPersistent) {
          this.initialStateSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').enable({emitEvent: false});
        } else {
          this.initialStateSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').disable({emitEvent: false});
        }
        break;
      case RpcInitialStateAction.GET_ATTRIBUTE:
        this.initialStateSettingsFormGroup.get('getAttribute').enable({emitEvent: false});
        break;
      case RpcInitialStateAction.GET_TIME_SERIES:
        this.initialStateSettingsFormGroup.get('getTimeSeries').enable({emitEvent: false});
        break;
    }
    if (action === RpcInitialStateAction.DO_NOTHING) {
      this.initialStateSettingsFormGroup.get('dataToState').disable({emitEvent: false});
    } else {
      this.initialStateSettingsFormGroup.get('dataToState').enable({emitEvent: false});
      if (dataToStateType === RpcDataToStateType.FUNCTION) {
        this.initialStateSettingsFormGroup.get('dataToState').get('dataToStateFunction').enable({emitEvent: false});
      } else {
        this.initialStateSettingsFormGroup.get('dataToState').get('dataToStateFunction').disable({emitEvent: false});
      }
    }
  }
}

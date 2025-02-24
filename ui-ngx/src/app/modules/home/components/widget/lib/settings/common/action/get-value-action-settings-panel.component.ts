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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge } from 'rxjs';
import {
  DataToValueType,
  GetValueAction,
  getValueActionsByWidgetType,
  getValueActionTranslations,
  GetValueSettings
} from '@shared/models/action-widget-settings.models';
import { ValueType } from '@shared/models/constants';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslationsShort } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetService } from '@core/http/widget.service';
import { AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-get-value-action-settings-panel',
  templateUrl: './get-value-action-settings-panel.component.html',
  providers: [],
  styleUrls: ['./action-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class GetValueActionSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  getValueSettings: GetValueSettings<any>;

  @Input()
  panelTitle: string;

  @Input()
  valueType: ValueType;

  @Input()
  trueLabel: string;

  @Input()
  falseLabel: string;

  @Input()
  stateLabel: string;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  @Input()
  popover: TbPopoverComponent<GetValueActionSettingsPanelComponent>;

  @Output()
  getValueSettingsApplied = new EventEmitter<GetValueSettings<any>>();

  getValueAction = GetValueAction;

  getValueActions: GetValueAction[];

  getValueActionTranslationsMap = getValueActionTranslations;

  telemetryTypeTranslationsMap = telemetryTypeTranslationsShort;

  attributeScopes = Object.keys(AttributeScope) as AttributeScope[];

  dataKeyType = DataKeyType;

  dataToValueType = DataToValueType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  ValueType = ValueType;

  getValueSettingsFormGroup: UntypedFormGroup;

  entityType = EntityType;

  alarmSeverities = Object.keys(AlarmSeverity) as AlarmSeverity[];
  alarmSeverityTranslationMap = alarmSeverityTranslations;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.getValueActions = getValueActionsByWidgetType(this.widgetType);
    this.getValueSettingsFormGroup = this.fb.group(
      {
        action: [this.getValueSettings?.action, []],
        defaultValue: [this.getValueSettings?.defaultValue, [Validators.required]],
        executeRpc: this.fb.group({
          method: [this.getValueSettings?.executeRpc?.method, [Validators.required]],
          requestTimeout: [this.getValueSettings?.executeRpc?.requestTimeout, [Validators.required, Validators.min(5000)]],
          requestPersistent: [this.getValueSettings?.executeRpc?.requestPersistent, []],
          persistentPollingInterval:
            [this.getValueSettings?.executeRpc?.persistentPollingInterval, [Validators.required, Validators.min(1000)]]
        }),
        getAttribute: this.fb.group({
          scope: [this.getValueSettings?.getAttribute?.scope, []],
          key: [this.getValueSettings?.getAttribute?.key, [Validators.required]]
        }),
        getTimeSeries: this.fb.group({
          key: [this.getValueSettings?.getTimeSeries?.key, [Validators.required]]
        }),
        getAlarmStatus: this.fb.group({
          severityList: [this.getValueSettings?.getAlarmStatus?.severityList],
          typeList: [this.getValueSettings?.getAlarmStatus?.typeList]
        }),
        dataToValue: this.fb.group({
          type: [this.getValueSettings?.dataToValue?.type, [Validators.required]],
          dataToValueFunction: [this.getValueSettings?.dataToValue?.dataToValueFunction, [Validators.required]],
        }),
      }
    );
    if (this.valueType === ValueType.BOOLEAN) {
      (this.getValueSettingsFormGroup.get('dataToValue') as UntypedFormGroup).addControl(
        'compareToValue', this.fb.control(this.getValueSettings?.dataToValue?.compareToValue, [Validators.required])
      );
    }

    merge(this.getValueSettingsFormGroup.get('action').valueChanges,
          this.getValueSettingsFormGroup.get('dataToValue').get('type').valueChanges,
          this.getValueSettingsFormGroup.get('executeRpc').get('requestPersistent').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyGetValueSettings() {
    const getValueSettings: GetValueSettings<any> = this.getValueSettingsFormGroup.getRawValue();
    this.getValueSettingsApplied.emit(getValueSettings);
  }

  getParseValueFunctionHelpId(): string {
    const action: GetValueAction = this.getValueSettingsFormGroup.get('action').value;
    if (action === GetValueAction.GET_DASHBOARD_STATE_OBJECT) {
      return 'widget/config/parse_value_get_dashboard_state_object_fn';
    }
    return 'widget/lib/rpc/parse_value_fn';
  }

  private updateValidators() {
    const action: GetValueAction = this.getValueSettingsFormGroup.get('action').value;
    let dataToValueType: DataToValueType = this.getValueSettingsFormGroup.get('dataToValue').get('type').value;

    this.getValueSettingsFormGroup.get('defaultValue').disable({emitEvent: false});
    this.getValueSettingsFormGroup.get('executeRpc').disable({emitEvent: false});
    this.getValueSettingsFormGroup.get('getAttribute').disable({emitEvent: false});
    this.getValueSettingsFormGroup.get('getTimeSeries').disable({emitEvent: false});
    this.getValueSettingsFormGroup.get('getAlarmStatus').disable({emitEvent: false});
    switch (action) {
      case GetValueAction.DO_NOTHING:
        this.getValueSettingsFormGroup.get('defaultValue').enable({emitEvent: false});
        break;
      case GetValueAction.EXECUTE_RPC:
        this.getValueSettingsFormGroup.get('executeRpc').enable({emitEvent: false});
        const requestPersistent: boolean = this.getValueSettingsFormGroup.get('executeRpc').get('requestPersistent').value;
        if (requestPersistent) {
          this.getValueSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').enable({emitEvent: false});
        } else {
          this.getValueSettingsFormGroup.get('executeRpc').get('persistentPollingInterval').disable({emitEvent: false});
        }
        break;
      case GetValueAction.GET_ATTRIBUTE:
        this.getValueSettingsFormGroup.get('getAttribute').enable({emitEvent: false});
        break;
      case GetValueAction.GET_TIME_SERIES:
        this.getValueSettingsFormGroup.get('getTimeSeries').enable({emitEvent: false});
        break;
      case GetValueAction.GET_ALARM_STATUS:
        this.getValueSettingsFormGroup.get('getAlarmStatus').enable({emitEvent: false});
        break;
      case GetValueAction.GET_DASHBOARD_STATE_OBJECT:
        this.getValueSettingsFormGroup.get('dataToValue.type').setValue(DataToValueType.FUNCTION, {emitEvent: false});
        dataToValueType = DataToValueType.FUNCTION;
        break
    }
    if (action === GetValueAction.DO_NOTHING || action === GetValueAction.GET_ALARM_STATUS) {
      this.getValueSettingsFormGroup.get('dataToValue').disable({emitEvent: false});
    } else {
      this.getValueSettingsFormGroup.get('dataToValue').enable({emitEvent: false});
      if (dataToValueType === DataToValueType.FUNCTION) {
        this.getValueSettingsFormGroup.get('dataToValue').get('dataToValueFunction').enable({emitEvent: false});
      } else {
        this.getValueSettingsFormGroup.get('dataToValue').get('dataToValueFunction').disable({emitEvent: false});
      }
    }
  }
}

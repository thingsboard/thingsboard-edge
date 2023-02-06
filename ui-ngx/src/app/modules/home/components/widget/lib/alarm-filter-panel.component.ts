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

import { Component, Inject, InjectionToken } from '@angular/core';
import {
  AlarmSearchStatus,
  alarmSearchStatusTranslations,
  AlarmSeverity,
  alarmSeverityTranslations
} from '@shared/models/alarm.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { OverlayRef } from '@angular/cdk/overlay';

export const ALARM_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface AlarmFilterPanelData {
  statusList: AlarmSearchStatus[];
  severityList: AlarmSeverity[];
  typeList: string[];
}

@Component({
  selector: 'tb-alarm-filter-panel',
  templateUrl: './alarm-filter-panel.component.html',
  styleUrls: ['./alarm-filter-panel.component.scss']
})
export class AlarmFilterPanelComponent {

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  alarmFilterFormGroup: FormGroup;

  result: AlarmFilterPanelData;

  alarmSearchStatuses = [AlarmSearchStatus.ACTIVE,
    AlarmSearchStatus.CLEARED,
    AlarmSearchStatus.ACK,
    AlarmSearchStatus.UNACK];

  alarmSearchStatusTranslationMap = alarmSearchStatusTranslations;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;

  alarmSeverityTranslationMap = alarmSeverityTranslations;

  constructor(@Inject(ALARM_FILTER_PANEL_DATA)
              public data: AlarmFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: FormBuilder) {
    this.alarmFilterFormGroup = this.fb.group(
      {
        alarmStatusList: [this.data.statusList],
        alarmSeverityList: [this.data.severityList],
        alarmTypeList: [this.data.typeList]
      }
    );
  }

  public alarmTypeList(): string[] {
    return this.alarmFilterFormGroup.get('alarmTypeList').value;
  }

  public removeAlarmType(type: string): void {
    const types: string[] = this.alarmFilterFormGroup.get('alarmTypeList').value;
    const index = types.indexOf(type);
    if (index >= 0) {
      types.splice(index, 1);
      this.alarmFilterFormGroup.get('alarmTypeList').setValue(types);
      this.alarmFilterFormGroup.get('alarmTypeList').markAsDirty();
    }
  }

  public addAlarmType(event: MatChipInputEvent): void {
    const input = event.input;
    const value = event.value;

    const types: string[] = this.alarmFilterFormGroup.get('alarmTypeList').value;

    if ((value || '').trim()) {
      types.push(value.trim());
      this.alarmFilterFormGroup.get('alarmTypeList').setValue(types);
      this.alarmFilterFormGroup.get('alarmTypeList').markAsDirty();
    }

    if (input) {
      input.value = '';
    }
  }

  update() {
    this.result = {
      statusList: this.alarmFilterFormGroup.get('alarmStatusList').value,
      severityList: this.alarmFilterFormGroup.get('alarmSeverityList').value,
      typeList: this.alarmFilterFormGroup.get('alarmTypeList').value
    };
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }
}


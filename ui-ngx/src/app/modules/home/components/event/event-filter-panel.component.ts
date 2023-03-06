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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { OverlayRef } from '@angular/cdk/overlay';
import { EntityType } from '@shared/models/entity-type.models';
import { FilterEventBody } from '@shared/models/event.models';
import { deepTrim } from '@core/utils';

export const EVENT_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface EventFilterPanelData {
  filterParams: FilterEventBody;
  columns: Array<FilterEntityColumn>;
}

export interface FilterEntityColumn {
  key: string;
  title: string;
}


@Component({
  selector: 'tb-event-filter-panel',
  templateUrl: './event-filter-panel.component.html',
  styleUrls: ['./event-filter-panel.component.scss']
})
export class EventFilterPanelComponent {

  eventFilterFormGroup: UntypedFormGroup;
  result: EventFilterPanelData;

  private conditionError = false;

  private msgDirectionTypes = ['IN', 'OUT'];
  private statusTypes = ['Success', 'Failure'];
  private msgTypes = ['Uplink', 'Downlink'];
  private entityTypes = Object.keys(EntityType);

  showColumns: FilterEntityColumn[] = [];

  constructor(@Inject(EVENT_FILTER_PANEL_DATA)
              public data: EventFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: UntypedFormBuilder) {
    this.eventFilterFormGroup = this.fb.group({});
    this.data.columns.forEach((column) => {
      this.showColumns.push(column);
      const validators = [];
      if (this.isNumberFields(column.key)) {
        validators.push(Validators.min(0));
      }
      this.eventFilterFormGroup.addControl(column.key, this.fb.control(this.data.filterParams[column.key] || '', validators));
      if (column.key === 'isError') {
        this.conditionError = true;
      }
    });
  }

  isSelector(key: string): string {
    return ['msgDirectionType', 'status', 'type', 'entityName'].includes(key) ? key : '';
  }

  isNumberFields(key: string): string {
    return ['minMessagesProcessed', 'maxMessagesProcessed', 'minErrorsOccurred', 'maxErrorsOccurred'].includes(key) ? key : '';
  }

  selectorValues(key: string): string[] {
    switch (key) {
      case 'msgDirectionType':
        return this.msgDirectionTypes;
      case 'status':
        return this.statusTypes;
      case 'type':
        return this.msgTypes;
      case 'entityName':
        return this.entityTypes;
    }
  }

  update() {
    const filter = deepTrim(Object.fromEntries(Object.entries(this.eventFilterFormGroup.value).filter(([_, v]) => v !== '')));
    this.result = {
      filterParams: filter,
      columns: this.data.columns
    };
    this.overlayRef.dispose();
  }

  showErrorMsgFields() {
    return !this.conditionError || this.eventFilterFormGroup.get('isError').value !== '';
  }

  cancel() {
    this.overlayRef.dispose();
  }

  changeIsError(value: boolean | string) {
    if (this.conditionError && value === '') {
      this.eventFilterFormGroup.get('errorStr').reset('', {emitEvent: false});
    }
  }
}


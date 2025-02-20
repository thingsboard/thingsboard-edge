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

import { AfterViewInit, Component, Inject, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { CalculatedFieldEventBody, DebugEventType, EventType } from '@shared/models/event.models';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { CalculatedFieldDebugDialogData, CalculatedFieldType } from '@shared/models/calculated-field.models';

@Component({
  selector: 'tb-calculated-field-debug-dialog',
  styleUrls: ['calculated-field-debug-dialog.component.scss'],
  templateUrl: './calculated-field-debug-dialog.component.html',
})
export class CalculatedFieldDebugDialogComponent extends DialogComponent<CalculatedFieldDebugDialogComponent, string> implements AfterViewInit {

  @ViewChild(EventTableComponent, {static: true}) eventsTable: EventTableComponent;

  readonly DebugEventType = DebugEventType;
  readonly debugEventTypes = DebugEventType;
  readonly EventType = EventType;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDebugDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldDebugDialogComponent, string>) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit(): void {
    this.eventsTable.entitiesTable.updateData();
    this.eventsTable.entitiesTable.cellActionDescriptors[0].isEnabled = () => this.data.value.type === CalculatedFieldType.SCRIPT;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  onDebugEventSelected(event: CalculatedFieldEventBody): void {
    this.data.getTestScriptDialogFn(this.data.value, JSON.parse(event.arguments))
      .subscribe(expression => this.dialogRef.close(expression));
  }
}

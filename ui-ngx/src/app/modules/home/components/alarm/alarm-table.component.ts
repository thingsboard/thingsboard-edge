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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DialogService } from '@core/services/dialog.service';
import { AlarmTableConfig } from './alarm-table-config';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { AlarmService } from '@app/core/http/alarm.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-alarm-table',
  templateUrl: './alarm-table.component.html',
  styleUrls: ['./alarm-table.component.scss']
})
export class AlarmTableComponent implements OnInit {

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    this.entityIdValue = entityId;
    if (this.alarmTableConfig && this.alarmTableConfig.entityId !== entityId) {
      this.alarmTableConfig.searchStatus = AlarmSearchStatus.ANY;
      this.alarmTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  alarmTableConfig: AlarmTableConfig;

  constructor(private alarmService: AlarmService,
              private dialogService: DialogService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.alarmTableConfig = new AlarmTableConfig(
      this.alarmService,
      this.dialogService,
      this.userPermissionsService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.entityIdValue,
      AlarmSearchStatus.ANY,
      this.store
    );
  }

}

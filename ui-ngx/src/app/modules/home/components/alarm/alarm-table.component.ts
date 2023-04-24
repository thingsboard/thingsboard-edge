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

import { ChangeDetectorRef, Component, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DialogService } from '@core/services/dialog.service';
import { AlarmTableConfig } from './alarm-table-config';
import { AlarmSearchStatus, AlarmSeverity, AlarmsMode } from '@shared/models/alarm.models';
import { AlarmService } from '@app/core/http/alarm.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActivatedRoute, Router } from '@angular/router';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { EntityService } from '@core/http/entity.service';
import { PageQueryParam } from '@shared/models/page/page-link';
import { AlarmFilterConfig } from '@shared/models/query/query.models';

interface AlarmPageQueryParams extends PageQueryParam {
  typeList?: Array<string>;
  statusList?: Array<AlarmSearchStatus>;
  severityList?: Array<AlarmSeverity>;
  assignedToMe?: boolean;
}

@Component({
  selector: 'tb-alarm-table',
  templateUrl: './alarm-table.component.html',
  styleUrls: ['./alarm-table.component.scss']
})
export class AlarmTableComponent implements OnInit {

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;
  alarmsMode = AlarmsMode.ENTITY;
  detailsMode = true;

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
      this.alarmTableConfig.alarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]};
      this.alarmTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }

  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  alarmTableConfig: AlarmTableConfig;

  constructor(private alarmService: AlarmService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef,
              private utilsService: UtilsService,
              private route: ActivatedRoute,
              private router: Router) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    const pageMode = !!this.route.snapshot.data.isPage;
    if (pageMode) {
      this.detailsMode = false;
    }
    if (isDefinedAndNotNull(this.route.snapshot.data.alarmsMode)) {
      this.alarmsMode = this.route.snapshot.data.alarmsMode;
    }
    const defaultAlarmFilterConfig: AlarmFilterConfig = {statusList: [AlarmSearchStatus.ACTIVE]};
    if (pageMode) {
      const routerQueryParams: AlarmPageQueryParams = this.route.snapshot.queryParams;
      if (routerQueryParams) {
        const queryParams = deepClone(routerQueryParams);
        let replaceUrl = false;
        if (routerQueryParams?.typeList) {
          defaultAlarmFilterConfig.typeList = routerQueryParams?.typeList;
          delete queryParams.typeList;
          replaceUrl = true;
        }
        if (routerQueryParams?.statusList) {
          defaultAlarmFilterConfig.statusList = routerQueryParams?.statusList;
          delete queryParams.statusList;
          replaceUrl = true;
        }
        if (routerQueryParams?.severityList) {
          defaultAlarmFilterConfig.severityList = routerQueryParams?.severityList;
          delete queryParams.severityList;
          replaceUrl = true;
        }
        if (routerQueryParams?.assignedToMe) {
          defaultAlarmFilterConfig.assignedToCurrentUser = routerQueryParams?.assignedToMe;
          delete queryParams.assignedToMe;
          replaceUrl = true;
        }
        if (replaceUrl) {
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams,
            queryParamsHandling: '',
            replaceUrl: true
          });
        }
      }
    }
    this.alarmTableConfig = new AlarmTableConfig(
      this.alarmService,
      this.entityService,
      this.dialogService,
      this.userPermissionsService,
      this.translate,
      this.datePipe,
      this.dialog,
      this.alarmsMode,
      this.entityIdValue,
      defaultAlarmFilterConfig,
      this.store,
      this.viewContainerRef,
      this.overlay,
      this.cd,
      this.utilsService,
      this.readonly,
      pageMode
    );
  }

}

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

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { TimePageLink } from '@shared/models/page/page-link';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AlarmInfo,
  AlarmQuery,
  AlarmSearchStatus,
  alarmSeverityColors,
  alarmSeverityTranslations,
  alarmStatusTranslations
} from '@app/shared/models/alarm.models';
import { AlarmService } from '@app/core/http/alarm.service';
import { DialogService } from '@core/services/dialog.service';
import { AlarmTableHeaderComponent } from '@home/components/alarm/alarm-table-header.component';
import {
  AlarmDetailsDialogComponent,
  AlarmDetailsDialogData
} from '@home/components/alarm/alarm-details-dialog.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { DAY, historyInterval } from '@shared/models/time/time.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { isUndefinedOrNull } from '@core/utils';

export class AlarmTableConfig extends EntityTableConfig<AlarmInfo, TimePageLink> {

  private authUser = getCurrentAuthUser(this.store);

  searchStatus: AlarmSearchStatus;

  constructor(private alarmService: AlarmService,
              private dialogService: DialogService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              public entityId: EntityId = null,
              private defaultSearchStatus: AlarmSearchStatus = AlarmSearchStatus.ANY,
              private store: Store<AppState>,
              private readonly) {
    super();
    this.loadDataOnInit = false;
    this.tableTitle = '';
    this.useTimePageLink = true;
    this.pageMode = false;
    this.defaultTimewindowInterval = historyInterval(DAY * 30);
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = true;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.actionsColumnTitle = 'alarm.details';
    this.entityType = EntityType.ALARM;
    this.entityTranslations = entityTypeTranslations.get(EntityType.ALARM);
    this.entityResources = {
    } as EntityTypeResource<AlarmInfo>;
    this.searchStatus = defaultSearchStatus;

    this.headerComponent = AlarmTableHeaderComponent;

    this.entitiesFetchFunction = pageLink => this.fetchAlarms(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<AlarmInfo>('createdTime', 'alarm.created-time', this.datePipe, '150px'));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('originatorName', 'alarm.originator', '25%',
        (entity) => entity.originatorName, entity => ({}), false));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('type', 'alarm.type', '25%'));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('severity', 'alarm.severity', '25%',
        (entity) => this.translate.instant(alarmSeverityTranslations.get(entity.severity)),
          entity => ({
            fontWeight: 'bold',
            color: alarmSeverityColors.get(entity.severity)
          })));
    this.columns.push(
      new EntityTableColumn<AlarmInfo>('status', 'alarm.status', '25%',
        (entity) => this.translate.instant(alarmStatusTranslations.get(entity.status))));

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('alarm.details'),
        icon: 'more_horiz',
        isEnabled: () => true,
        onAction: ($event, entity) => this.showAlarmDetails(entity)
      }
    );

    if (isUndefinedOrNull(this.readonly)) {
      this.readonly = !this.userPermissionsService.hasGenericPermission(Resource.ALARM, Operation.WRITE);
    }
  }

  fetchAlarms(pageLink: TimePageLink): Observable<PageData<AlarmInfo>> {
    const query = new AlarmQuery(this.entityId, pageLink, this.searchStatus, null, true);
    return this.alarmService.getAlarms(query);
  }

  showAlarmDetails(entity: AlarmInfo) {
    this.dialog.open<AlarmDetailsDialogComponent, AlarmDetailsDialogData, boolean>
    (AlarmDetailsDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          alarmId: entity.id.id,
          alarm: entity,
          allowAcknowledgment: !this.readonly,
          allowClear: !this.readonly,
          displayDetails: true
        }
      }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.updateData();
        }
      }
    );
  }
}
